import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Procesador{

    private static BufferedReader bf; // Buffer de lectura
    private static char car; // Caracter leido
    private static MT_AFD MT_AFD = new MT_AFD(); // Matriz de transiciones del AFD
    private static FileWriter FTokens;  // Fichero Tokens
    private static FileWriter FTS;      // Fichero Tabla de Simbolos
    private static FileWriter FErr;     // Fichero de Errores
    private static FileWriter FParse;   // Fichero de Parse
    private static String[] TPalRes = new String[10];   // Tabla de palabras reservadas
    private static int lineaActual = 1; // Numero de linea actual
    private static boolean zonaDec = true; // Estamos en zona de declaracion
    private static boolean TSGActiva = true; // La tabla de simbolos global esta activa , false -> es la local
    private static ArrayList<Object> TSG = new  ArrayList<Object>();//Tabla de Símbolos Global
    private static Map<String, Integer> existeTSG = new HashMap<String,Integer>(); // Comprobar si existe el identificador
    private static ArrayList<Object> TSL = new  ArrayList<Object>();//Tabla de Símbolos Local
    private static Map<String, Integer> existeTSL = new HashMap<String,Integer>(); // Comprobar si existe el identificador
    private static Token sig_tok = null;

    public static void main(String[] args) {
        if(args.length == 0 || args[0].equals("-h") || args[0].equals("-help")){
            System.out.printf("Uso: java -jar procesador.java input.txt\n"); System.exit(1);
        }
        else if(args[0].equals("info")){
            System.out.printf("procesador.java\nImplementacion del Procesador de Lenguaje para JavaScript-PDL\nAutores: Grupo 29\n\tAlvaro Alonso Miguel, Javier Sabin Gomez\n"); System.exit(1);
        }

        // Se abre el fichero fuente
        try{ bf = new BufferedReader(new FileReader(args[0]));}
        catch(FileNotFoundException e){ System.err.println("Error: Fichero no encontrado"); System.exit(1);}

        // Se crean los ficheros
        try{
            FTokens = new FileWriter(new File("tokens.txt")); //Fichero de Tokens
            FTS = new FileWriter(new File("TS.txt")); //Fichero de Tabla de Simbolos
            FErr = new FileWriter(new File("errores.txt")); //Fichero de Errores
            FParse = new FileWriter(new File("parse.txt")); //Fichero de Parse

            FErr.write("ERRORES DEL FICHERO FUENTE: \n\n");
            FParse.write("DescendenteRecursivo");

            car = (char)bf.read();
        }catch(IOException e){e.printStackTrace();}

        llenarTablas(); // Llenamos las tablas

        AnSin();    // Llamo al ASin

        writeTS();  // Vuelco las tablas

            try {
                FTokens.close();
                FTS.close();
                FErr.close();
                FParse.write("\n");
                FParse.close();
                bf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    private static void llenarTablas(){
        // Palabras reservadas
        TPalRes[0] = "boolean";
        TPalRes[1] = "int";
        TPalRes[2] = "string";
        TPalRes[3] = "if";
        TPalRes[4] = "while";
        TPalRes[5] = "function";
        TPalRes[6] = "input";
        TPalRes[7] = "let";
        TPalRes[8] = "print";
        TPalRes[9] = "return";

        // Estado 0
        // del
        MT_AFD.addValor(0, '\t', new Par(0, ' '));
        MT_AFD.addValor(0, 32, new Par(0, ' '));
        MT_AFD.addValor(0, '\n', new Par(0, ' '));
        MT_AFD.addValor(0, '\r', new Par(0, ' '));
        MT_AFD.addValor(0, 0, new Par(0, ' '));
        // letras
        for(int i='A'; i<='z'; i++){
            if((i >= 'A' && i <= 'Z') || (i >= 'a' && i<= 'z'))
                MT_AFD.addValor(0, i, new Par(1,'A'));
        }
        //digitos
        for (int i = '0'; i <= '9' ; i++){
            MT_AFD.addValor(0, i, new Par(2, 'B'));
        }    
        // restantes
        MT_AFD.addValor(0, '"', new Par(3, ' '));
        MT_AFD.addValor(0, '/', new Par(4, ' '));
        MT_AFD.addValor(0, '=', new Par(11, 'G'));
        MT_AFD.addValor(0, ',', new Par(12, 'H'));
        MT_AFD.addValor(0, ';', new Par(13, 'I'));
        MT_AFD.addValor(0, '(', new Par(14, 'J'));
        MT_AFD.addValor(0, ')', new Par(15, 'K'));
        MT_AFD.addValor(0, '{', new Par(16, 'L'));
        MT_AFD.addValor(0, '}', new Par(17, 'M'));
        MT_AFD.addValor(0, '+', new Par(18, 'N'));
        MT_AFD.addValor(0, '!', new Par(19, 'O'));
        MT_AFD.addValor(0, '>', new Par(20, 'P'));

        // Estado 1
        // letras
        for(int i='A'; i<='z'; i++){
            if((i >= 'A' && i <= 'Z') || (i >= 'a' && i<= 'z'))
                MT_AFD.addValor(1, i, new Par(1,'A'));
        }
        //digitos
        for (int i = '0'; i <= '9' ; i++){
            MT_AFD.addValor(1, i, new Par(1, 'A'));
        }    
        // _
        MT_AFD.addValor(1, '_', new Par(1, 'A'));
        // oc
        for (int i = 0; i <= 126; i++) {
			if((i >= 0 && i <= '/') || (i >= ':' && i <='@') 
					|| (i >= '[' && i <= 94	) || (i >= '{' && i <= '}'))
				MT_AFD.addValor(1, i, new Par(7,'C'));
		}

        // Estado 2
        //digitos
        for (int i = '0'; i <= '9' ; i++){
            MT_AFD.addValor(2, i, new Par(2, 'B'));
        }
        // oc
		for (int i = 0; i <= 126; i++) {
			if((i >= 0 && i <= '/') || (i >= ':' && i <='}'))
				MT_AFD.addValor(2, i, new Par(8, 'D'));
        }

        // Estado 3
        MT_AFD.addValor(3, '"', new Par(9,'E'));
        // c
        for (int i = 32; i <= 126; i++) {
			if((i >= 32 && i <= '!') || (i >= '#' && i <= 126))
				MT_AFD.addValor(3, i, new Par(3, 'A'));
        }

        // Estado 4
        MT_AFD.addValor(4, '=', new Par(10, 'F'));
        MT_AFD.addValor(4, '*', new Par(5, ' '));

        // Estado 5
        MT_AFD.addValor(5, '*', new Par(6, ' '));
		MT_AFD.addValor(5, '\t', new Par(5, ' '));
        MT_AFD.addValor(5, '\r', new Par(5, ' '));
		MT_AFD.addValor(5, '\n', new Par(5, ' '));
        MT_AFD.addValor(5, 32, new Par(5, ' '));
			// m
		for (int i = 32; i <= 126; i++) {
			if((i >= 32 && i <= ')') || (i >= '+' && i <= 126))
				MT_AFD.addValor(5, i, new Par(5, ' '));
		}
        //Estado 6
        MT_AFD.addValor(6, '/', new Par(0, ' '));
		MT_AFD.addValor(6, '*', new Par(6, ' '));
		MT_AFD.addValor(6, '\t', new Par(6, ' '));
		MT_AFD.addValor(6, '\n', new Par(6, ' '));
		MT_AFD.addValor(6, '\r', new Par(6, ' '));
        MT_AFD.addValor(6, 32, new Par(6, ' '));
			// m
		for (int i = 32; i <= 126; i++) {
			if((i >= 32 && i <= ')') || (i >= '+' && i <= 46)
				|| (i >= 48 && i <= 126))
				MT_AFD.addValor(6, i, new Par(5, ' '));
		}
    }
    private static Token AnLex(){
        Token token = null;
        int estado = 0;
        char accion;
        String lexema = "";
        String valor = "";

        while(estado<7){ // Mientras no sea un estado final
            if((int) car == 65535 || car == 0){ // Final del fichero
                return new Token(65535, "$");
            }
            else if(car == '\n'){ // Nueva linea
                lineaActual++;
            }
            if(car >= 128) {gestorErrores("AnLex", 6, String.valueOf(car), ""); leer();} // Caracter no contemplado
            Par p = MT_AFD.getValor(estado, car);
            if(p == null){ // Error: se ha leido un caracter no esperado
                if(estado == 4)
                    gestorErrores("AnLex", 5, "", "");
                else
                    gestorErrores("AnLex", 3, String.valueOf(car), "");
                leer();
                return null;
            }
            estado = p.estado(); accion = p.accion();
            if(accion == ' '){  // No hay transicion
                leer();
            }
            else{
                char AccSem = accion;
                switch(AccSem){
                    case 'A':
                        lexema += car; leer(); break;
                    case 'B':
                        valor += car; leer(); break;
                    case 'C':
                        int pos = buscarTPalRes(lexema);
                        if(pos != -1) token = genToken(pos, null); // Lexema es una palabra reservada
                        else{
                            boolean found;
                            if(zonaDec){//Estamos en zona de declaración
                                if(TSGActiva) found = existeTSG.containsKey(lexema);  
                                else found = existeTSL.containsKey(lexema);
                                if(found){
                                    if(TSGActiva) token = genToken(13, Integer.toString(existeTSG.get(lexema))); // Creo el token con su posicion en la TS
                                    else token = genToken(13, Integer.toString(existeTSL.get(lexema)));
                                    //gestorErrores("AnLex", 4, lexema);    // Variable ya declarada
                                }
                                else{
                                    ArrayList<Object> atribs = new ArrayList<Object>();
                                    atribs.add(0, lexema);
                                    atribs.add(1, ""); atribs.add(2, ""); atribs.add(3, "");
                                    ArrayList<String> tipoParam = new ArrayList<String>();
                                    atribs.add(4, tipoParam); atribs.add(5, ""); atribs.add(6, "");
                                    if(TSGActiva){  // TSG activa
                                        pos = TSG.size();
                                        existeTSG.put(lexema, TSG.size());
                                        TSG.add(atribs);
                                    }  
                                    else {  // TSL activa
                                        pos = TSL.size();
                                        existeTSL.put(lexema,TSL.size());  
                                        TSL.add(atribs);
                                    }    
                                    token = genToken(13, Integer.toString(pos)); // Token (id, posTS)
                                }
                            }
                            else{ // No estoy en zona de declaracion
                                if(!existeTSG.containsKey(lexema) && !existeTSL.containsKey(lexema)){ gestorErrores("AnLex", 7, lexema ,"");} // No esta declarada
                                else {
                                    if(TSGActiva) token = genToken(13, Integer.toString(existeTSG.get(lexema))); // Creo el token con su posicion en la TS
                                    else token = genToken(13, Integer.toString(existeTSL.get(lexema)));
                                }
                            }
                        }
                        break;
                    case 'D' : // Token constante entera 
                        if(Integer.valueOf(valor) > 32767) gestorErrores("AnLex", 1, "", "");
                        else token = genToken(11, valor); break;
                    case 'E' : // Token cadena
                        if(lexema.length() > 64) gestorErrores("AnLex", 2, "", "");
                        else token = genToken(12, "\""+lexema+"\""); leer(); break;
                    case 'F' : // Token asignación con división
                        if(car == '=')
                            token = genToken(15, null);
                        else if(car != '*') gestorErrores("AnLex", 5, "", "");
                        leer(); break;
                    case 'G' : // Token asignacion
                        token = genToken(14, null); leer(); break;
                    case 'H' : // Token coma
                        token = genToken(16, null); leer(); break;
                    case 'I' : // Token puntoC
                        token = genToken(17, null); leer(); break;
                    case 'J' : // Token parA
                        token = genToken(18, null); leer(); break;
                    case 'K' : // Token parC
                        token = genToken(19, null); leer(); break;
                    case 'L' : // Token llaveA
                        token = genToken(20, null); leer(); break;
                    case 'M' : // Token llaveC
                        token = genToken(21, null); leer(); break;
                    case 'N' : // Token suma
                        token = genToken(22, null); leer(); break;
                    case 'O' : // Token negacion
                        token = genToken(23, null); leer(); break;
                    case 'P' : // Token mayor
                        token = genToken(24, null); leer(); break;
                }
            }
        }

        return token;
    }
    private static void AnSin(){
        while(true){
            sig_tok = AnLex();
            if(sig_tok == null){
                gestorErrores("AnSin", 8, "", "");
                continue;
            }
            A();
            if("$".equals(sig_tok.atributo())){
                break;
            }
        }
    }
    private static void A(){
        // A -> B A
        if(sig_tok.id() == 8 || sig_tok.id() == 4 || sig_tok.id() == 5 || sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7){
            parse(1);
            B();
            A();
        }// A -> F A
        else if(sig_tok.id() == 6){
            parse(2);
            F();
            A();
        }// A -> lambda
        else if("$".equals(sig_tok.atributo())){
            parse(3);
        }
    }
    private static void B(){
        // B -> let T id B1 ;
        if(sig_tok.id() == 8){
            parse(4);
            equipara(8);
            T();
            equipara(13);
            B1();            
        }
        // B -> if ( E ) S
        else if(sig_tok.id() == 4){
            parse(5);
            equipara(4);
            equipara(18);
            E();
            equipara(19);
            S();
        }
        // B -> while ( E ) { C }
        else if(sig_tok.id() == 5){
            parse(6);
            equipara(5);
            equipara(18);
            E();
            equipara(19);
            equipara(20);
            C();
            equipara(21);
        }
        // B -> S
        else if(sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7 ){
            parse(7);
            S();
        }
    }
    private static void T(){
        // T -> int
        if(sig_tok.id() == 2){
            parse(12);
            equipara(2);
        }
        // T -> string
        else if(sig_tok.id() == 3){
            parse(13);
            equipara(3);
        }
        // T -> boolean
        else if(sig_tok.id() == 1){
            parse(14);
            equipara(1);
        }
    }
    private static void B1(){
        // B1 -> = E
        if(sig_tok.id() == 14){
            parse(8);
            equipara(14);
            E();
        }
        // B1 -> lambda
        else if(sig_tok.id() == 17){
            parse(9);
        }
        else{
            gestorErrores("AnSin", 9, "", "");
        }
    }
    private static void E(){
        // E -> E1 EX
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(24);
            E1();
            EX();
        }
    }
    private static void E1(){
        // E1 -> E2 EX1
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(26);
            E2();
            EX1();
        }
    }
    private static void EX(){
        // EX -> > E1 EX
        if(sig_tok.id() == 24){
            parse(25);
            equipara(24);
            E1();
            EX();
        } // EX -> lambda
        else if(sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(49);
        }
        else{
            gestorErrores("AnSin", 10, token_decoder(sig_tok.id()), "");
        }
    }
    private static void E2(){
        // E2 -> ! E2
        if(sig_tok.id() == 23){
            parse(28);
            equipara(23);
            E2();
        }
        // E2 -> E3
        else if(sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(29);
            E3();
        }
    }
    private static void E3(){
        // E3 -> ( E )
        if(sig_tok.id() == 18){
            parse(30);
            equipara(18);
            E();
            equipara(19);
        }// E3 -> id Q
        else if(sig_tok.id() == 13){
            parse(31);
            equipara(13);
            Q();
        }// E3 -> cad
        else if(sig_tok.id() == 12){
            parse(34);
            equipara(12);
        }// E3 -> ent
        else if(sig_tok.id() == 11){
            parse(35);
            equipara(11);
        }
    }
    private static void EX1(){
        // EX1 -> + E2 EX1
        if(sig_tok.id() == 22){
            parse(27);
            equipara(22);
            E2();
            EX1();
        }// EX1 -> lambda
        else if(sig_tok.id() == 24 || sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(50);
        }
        else{
            gestorErrores("AnSin", 11, token_decoder(sig_tok.id()), "");
        }
    }    
    private static void Q(){
        //Q -> ( L )
        if(sig_tok.id() == 18){
            parse(33);
            equipara(18);
            L();
            equipara(19);
        } // Q -> lambda
        else if(sig_tok.id() == 22 || sig_tok.id() == 24 || sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(32);
        }
        else{
            gestorErrores("AnSin", 11, token_decoder(sig_tok.id()), "");
        }
    }
    private static void F(){
        parse(40);
        // F -> F1 F2 F3
        F1();
        F2();
        F3();
    }
    private static void F1(){
        parse(41);
        // F1 -> function id H
        if(sig_tok.id() == 6){
            equipara(6);
            equipara(13);
            H();
        }
    }
    private static void F2(){
        parse(42);
        // F2 -> ( P )
        if(sig_tok.id() == 18){
            equipara(18);
            P();
            equipara(19);
        }
    }
    private static void F3(){
        parse(43);
        // F3 -> { C }
        if(sig_tok.id() == 20){
            equipara(20);
            C();
            equipara(21);
        }
    }
    private static void H(){
        // H -> T
        if(sig_tok.id() == 1 || sig_tok.id() == 2 || sig_tok.id() == 3){
            parse(44);
            T();
        } // H -> lambda
        else if(sig_tok.id() == 18){
            parse(51);
        }
        else{
            gestorErrores("AnSin", 20, "", "");
        }
    }
    private static void P(){
        // P -> T id P1
        if(sig_tok.id() == 2 || sig_tok.id() == 3 || sig_tok.id() == 1){
            parse(45);
            T();
            equipara(13);
            P1();
        } // P -> lambda
        else if (sig_tok.id() == 19){
            parse(46);
        }
        else{
            gestorErrores("AnSin", 13, "", "");
        }
    }
    private static void P1(){
        // P1 -> , T id P1
        if(sig_tok.id() == 16){
            parse(47);
            equipara(16);
            T();
            equipara(13);
            P1();
        }// P1 -> lambda
        else if(sig_tok.id() == 19){
            parse(48);
        }
        else{
            gestorErrores("AnSin", 14, "", "");
        }
    }
    private static void C(){
        // C -> B C
        if(sig_tok.id() == 8 || sig_tok.id() == 4 || sig_tok.id() == 5 || sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7){
            parse(10);
            B();
            C();
        }// C -> lambda
        else if(sig_tok.id() == 21){
            parse(11);
        }
        else{
            gestorErrores("AnSin", 15, "", "");
        }
    }
    private static void S(){
        // S -> id N
        if(sig_tok.id() == 13){
            parse(15);
            equipara(13);
            N();
        }// S -> return R ;
        else if(sig_tok.id() == 10){
            parse(19);
            equipara(10);
            R();
            equipara(17);
        }// S -> print ( E ) ;
        else if(sig_tok.id() == 9){
            parse(20);
            equipara(9);
            equipara(18);
            E();
            equipara(19);
            equipara(17);
        }// S -> input ( id ) ;
        else if(sig_tok.id() == 7){
            parse(21);
            equipara(7);
            equipara(18);
            equipara(13);
            equipara(19);
            equipara(17);
        }
    }
    private static void N(){
        //N -> ( L ) ;
        if(sig_tok.id() == 18){
            parse(16);
            equipara(18);
            L();
            equipara(19);
            equipara(17);
        }//N -> = E ;
        else if(sig_tok.id() == 14){
            parse(17);
            equipara(14);
            E();
            equipara(17);

        }//N -> /= E ;
        else if(sig_tok.id() == 15){
            parse(18);
            equipara(15);
            E();
            equipara(17);
        }
    }
    private static void R(){
        // R -> E
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(22);
            E(); 
        }// R -> lambda
        else if(sig_tok.id() == 17){
            parse(23);
        }
        else{
            gestorErrores("AnSin", 16, "", "");
        }
    }
    private static void L(){
        // L -> E L1
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(36);
            E();
            L1();
        } // L -> lambda
        else if(sig_tok.id() == 19){
            parse(37);
        }
        else{
            gestorErrores("AnSin", 17, "", "");
        }
    }
    private static void L1(){
        // L1 -> , E L1
        if(sig_tok.id() == 16){
            parse(38);
            equipara(16);
            E();
            L1();
        } // L1 -> lambda
        else if(sig_tok.id() == 19){
            parse(39);
        }
        else{
            gestorErrores("AnSin", 18, "", "");
        }    
    }
    private static Token genToken(int id, String atributo){
        String atr = atributo == null ? "" : atributo;
        String token = String.format("<%d,%s>", id,atr);
        try{ FTokens.write(token + "\n");}
        catch (IOException e){
            System.err.println("An error occurred");     
            e.printStackTrace();
        }    
        return new Token(id, atributo);
    }
    private static void leer(){
        try{ car = (char)bf.read();}
        catch (IOException e){ e.printStackTrace();}
    }
    private static int buscarTPalRes(String lex){
        for(int i=0; i<TPalRes.length; i++){
            if(TPalRes[i].equals(lex)) return i+1;
        }
        return -1;
    }
    private static void parse(int n){
        try{
            FParse.write(" "+n);
        }catch(IOException e){ System.err.println("No se pudo escribir el parse");}
    }
    private static void equipara(int n){
        if(sig_tok.id() == n){
            sig_tok = AnLex();
            if(sig_tok == null) { // Si me devuelve un error escribo todo y exit 1
                writeTS();  // Vuelco las tablas

                try {
                    FTokens.close();
                    FTS.close();
                    FErr.close();
                    FParse.write("\n");
                    FParse.close();
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        } 
        else{
            gestorErrores("AnSin", 19, token_decoder(sig_tok.id()), token_decoder(n));
        }
    }
    private static String token_decoder(int n){
        if(n <= 10){
            return TPalRes[n-1];
        }
        switch(n){
            case 11: return "ent"; 
            case 12: return "cad"; 
            case 13: return "id";
            case 14: return "=";
            case 15: return "/=";
            case 16: return ","; 
            case 17: return ";";
            case 18: return "("; 
            case 19: return ")";
            case 20: return "{";
            case 21: return "}";
            case 22: return "+";
            case 23: return "!";
            case 24: return ">";
            default: return "No existe token con este numero";
        }
    }
    private static void gestorErrores(String analiser, int nError, String token, String tok_obligatorio){
        String msg; int linea = lineaActual;
        if(analiser.equals("AnLex"))
            msg = String.format("Error léxico en linea %s: ", linea);
        else if(analiser.equals("AnSin"))
            msg = String.format("Error sintáctico en linea %s: ", linea);
        else
            msg = String.format("Error semántico %s: ", linea);

        try{
            switch(nError){
                case 1: 
                    FErr.write(msg +"Entero fuera de rango máximo de 32767\n"); break;
                case 2:
                    FErr.write(msg +"Cadena con mas de 64 caracteres\n"); break;
                case 3:
                    FErr.write(msg+"Caracter '"+ token +"' no esperado\n"); break;
                case 4:
                    FErr.write(msg+"Variable '"+ token +"' ya declarada\n"); break;
                case 5:
                    FErr.write(msg+"Se esperaba = o * despues de la /\n"); break;
                case 6:
                    FErr.write(msg+"Caracter "+token+" no contemplado en el lenguaje\n"); break;
                case 7: 
                    FErr.write(msg+"Variable '"+ token +"' no declarada\n"); break;
                case 8: 
                    FErr.write(msg+"No se ha podido leer ningun token\n"); break;
                case 9: 
                    FErr.write(msg+"La forma de declarar una variable es: let tipo nombre [= expresion];\n"); break;
                case 10: 
                    FErr.write(msg+"Se esperaba uno de los siguientes tokens {',' , ';' , ')'} y se ha recibido '"+ token +"'\n"); break;
                case 11: 
                    FErr.write(msg+"Se esperaba uno de los siguientes tokens {'>' , ',' , ';' , ')'} y se ha recibido '"+ token +"'\n"); break;
                case 12: 
                    FErr.write(msg+"Se esperaba uno de los siguientes tokens {'+', '>' ,' , ';' , ')'} y se ha recibido '"+ token +"'\n"); break;
                case 13: 
                    FErr.write(msg+"Para cerrar los parametros se hace uso de ')'\n"); break;
                case 14: 
                    FErr.write(msg+"La forma de declarar parametros es: (tipo id {, tipo id, ...})\n"); break;
                case 15: 
                    FErr.write(msg+"Para cerrar el contenido de una funcion se hace uso de '}'\n"); break;
                case 16: 
                    FErr.write(msg+"La forma de realizar un return es: return expresion ;\n"); break;
                case 17: 
                    FErr.write(msg+"Para cerrar las condiciones de una funcion se hace uso de ')'\n"); break;
                case 18: 
                    FErr.write(msg+"La forma de establecer las condiciones de una funcion es (expresion {, expresion , ...})\n"); break;
                case 19: 
                    FErr.write(msg+"Se esperaba el token '" + tok_obligatorio + "' y se ha recibido el token '"+ token+ "'\n"); break;
                case 20: 
                    FErr.write(msg+"Para abrir los parametros se hace uso de '('\n"); break;
            }
        } catch(IOException e) {e.printStackTrace();}
    }
    @SuppressWarnings("unchecked")
    private static void writeTS(){
        ArrayList<Object> atribs;
        String lex;
        ArrayList<Object> ts;
        
        ts = TSG;
        try {
            FTS.write("TABLA DE SIMBOLOS GLOBAL #1:\n");
            while(!ts.isEmpty()){
                atribs = (ArrayList<Object>)ts.remove(0);
                lex = (String)atribs.get(0);
                FTS.write("\n*LEXEMA: '"+lex+"'\n");
                FTS.write("\tATRIBUTOS: \n");
                for(int j=1; j<7; j++){
                    switch(j){
                        case 1:
                            if(!atribs.get(j).equals("")) FTS.write("\t+tipo:\t\t\t'"+atribs.get(j)+"'\n"); break;
                        case 2:
                            if(!atribs.get(j).equals("")) FTS.write("\t+despl:\t\t\t'"+atribs.get(j)+"'\n"); break;
                        case 3:
                            if(!atribs.get(j).equals("")) FTS.write("\t+numParam:\t\t\t'"+atribs.get(j)+"'\n"); break;
                        case 4:
                            ArrayList<String> tipoParam = (ArrayList<String>) atribs.get(j);
                            if(!tipoParam.isEmpty())
                                for(int k=0; k<tipoParam.size(); k++)
                                    FTS.write("\t+tipoParam"+0+(k+1)+":\t'"+tipoParam.get(k)+"'\n"); break;
                        case 5:
                            if(!atribs.get(j).equals("")) FTS.write("\t+tipoRet:\t\t\t'"+atribs.get(j)+"'\n"); break;
                        case 6:
                            if(!atribs.get(j).equals("")) FTS.write("\t+etiqFun:\t\t\t'"+atribs.get(j)+"'\n"); break;
                    }
                }
            }
            //FTS.write("\n---------------------------------\n");
        } catch (IOException e) { e.printStackTrace();}
    }
}