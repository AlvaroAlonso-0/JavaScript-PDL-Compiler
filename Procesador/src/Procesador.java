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
    private static boolean zonaDec = false; // Estamos en zona de declaracion
    public static String lexema = "";
    private static boolean TSGActiva = true; // La tabla de simbolos global esta activa , false -> es la local
    private static ArrayList<Object> TSG = new  ArrayList<Object>();//Tabla de Símbolos Global
    private static Map<String, Integer> existeTSG = new HashMap<String,Integer>(); // Comprobar si existe el identificador
    private static ArrayList<Object> TSL = new  ArrayList<Object>();//Tabla de Símbolos Local
    private static Map<String, Integer> existeTSL = new HashMap<String,Integer>(); // Comprobar si existe el identificador
    private static ArrayList<Object> TS = new ArrayList<Object>(); //Todas las tablas locales
    private static ArrayList<String> TSName = new ArrayList<String>(); // Nombres de las tablas
    private static int despG = 0, despL = 0, funActual = 0, nAmbitos = 1;
    private static Token sig_tok = null; // Siguiente token
    private static final String vac = "vac", fun = "fun", log = "log", cad = "cad", ent = "ent", ok = "tipo_ok", err = "tipo_error"; // Tipos Semantico

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
        lexema = "";
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
                                    gestorErrores("AnLex", 4, lexema, "");    // Variable ya declarada
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
                                if(!existeTSG.containsKey(lexema) && !existeTSL.containsKey(lexema)){token = genToken(13, "0"); gestorErrores("AnLex", 7, lexema ,"");} // No esta declarada
                                else {
                                    if(existeTSG.containsKey(lexema)) token = genToken(13, Integer.toString(existeTSG.get(lexema))); // Creo el token con su posicion en la TS
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
    private static String A(){
        // A -> B A
        if(sig_tok.id() == 8 || sig_tok.id() == 4 || sig_tok.id() == 5 || sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7){
            parse(1);
            String b = B();
            String a = A();
            if(b.equals(vac)){ return a;}
            else if(b.equals(err) || a.equals(err)) return err;
        }// A -> F A
        else if(sig_tok.id() == 6){
            parse(2);
            String f = F();
            String a = A();
            if(f.equals(vac)){ return a;}
            else if(f.equals(err) || a.equals(err)) return err;
        }// A -> lambda
        else if("$".equals(sig_tok.atributo())){
            parse(3);
            return vac;
        }
        return ok;
    }
    private static String B(){
        // B -> let T id B1 ;
        if(sig_tok.id() == 8){
            parse(4);
            zonaDec = true;
            equipara(8);
            String[] t = T().split(" ");
            int pos = Integer.parseInt(sig_tok.atributo());
            equipara(13);
            zonaDec = false;
            if(!TSGActiva){ 
                insertarTipoTS(pos, t[0], TSL);
                insertarDespTS(pos, despL, TSL);
                despL+=Integer.parseInt(t[1]);
            } else{
                insertarTipoTS(pos, t[0], TSG);
                insertarDespTS(pos, despG, TSG);
                despG+=Integer.parseInt(t[1]);
            }
            String bOne = B1(); 
            equipara(17);         
            if(bOne.equals(vac) || bOne.equals(t[0])){ return ok;}
            else{gestorErrores("AnSem", 21, "", ""); return err;}  
        }
        // B -> if ( E ) S
        else if(sig_tok.id() == 4){
            parse(5);
            equipara(4);
            equipara(18);
            String e = E();
            if(!e.equals(log)){gestorErrores("AnSem", 24, "", ""); return err;}
            equipara(19);
            String s = S();
            return s.split(" ")[0];
        }
        // B -> while ( E ) { C }
        else if(sig_tok.id() == 5){
            parse(6);
            equipara(5);
            equipara(18);
            String e = E();
            if(!e.equals(log)){gestorErrores("AnSem", 24, "", ""); return err;}
            equipara(19);
            equipara(20);
            String c = C();
            equipara(21);
            return c;
        }
        // B -> S
        else if(sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7 ){
            parse(7);
            String s = S();
            return s;
        }
        return err;
    }
    private static String T(){
        // T -> int
        if(sig_tok.id() == 2){
            parse(12);
            equipara(2);
            return (ent+" 1");
        }
        // T -> string
        else if(sig_tok.id() == 3){
            parse(13);
            equipara(3);
            return (cad+" 64");
        }
        // T -> boolean
        else if(sig_tok.id() == 1){
            parse(14);
            equipara(1);
            return (log+" 1");
        }
        gestorErrores("AnSem", 22, "", "");
        return err + " 0";
    }
    private static String B1(){
        // B1 -> = E
        if(sig_tok.id() == 14){
            parse(8);
            equipara(14);
            return E();
        }
        // B1 -> lambda
        else if(sig_tok.id() == 17){
            parse(9);
            return vac;
        }
        else{
            gestorErrores("AnSin", 9, "", "");
            return err;
        }
    }
    private static String E(){
        // E -> E1 EX
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(24);
            String eOne = E1();
            String ex = EX();
            if(eOne.equals(ent) && ex.equals(log)) return log;
            else return eOne;
        }
        return err;
    }
    private static String E1(){
        // E1 -> E2 EX1
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(26);
            String e = E2();
            String ex = EX1();
            if(e.equals(ent) && ex.equals(ent)) return ent;
            else return e;
        }
        return err;
    }
    private static String EX(){
        // EX -> > E1 EX
        if(sig_tok.id() == 24){
            parse(25);
            equipara(24);
            String e = E1();
            String ex = EX();
            if(e.equals(ent) && (ex.equals(ent) || ex.equals(vac))) return log;
            else{ gestorErrores("AnSem", 23, "", ""); return err;}
        } // EX -> lambda
        else if(sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(49);
            return vac;
        }
        else{
            gestorErrores("AnSin", 10, token_decoder(sig_tok.id()), "");
            return err;
        }
    }
    private static String E2(){
        // E2 -> ! E2
        if(sig_tok.id() == 23){
            parse(28);
            equipara(23);
            String e = E2();
            if(e.equals(log)) return log;
            else{gestorErrores("AnSem", 24, "", ""); return err;}
        }
        // E2 -> E3
        else if(sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(29);
            return E3();
        }
        return err;
    }
    @SuppressWarnings("unchecked")
    private static String E3(){
        // E3 -> ( E )
        if(sig_tok.id() == 18){
            parse(30);
            equipara(18);
            String e = E();
            equipara(19);
            return e;
        }// E3 -> id Q
        else if(sig_tok.id() == 13){
            parse(31);
            int pos = Integer.parseInt(sig_tok.atributo());
            String lex = lexema;
            boolean existe = TSGActiva ? existeTSG.containsKey(lexema) : existeTSL.containsKey(lexema) || existeTSG.containsKey(lexema);
            equipara(13);
            if(!existe){
                ArrayList<Object> pointer = new ArrayList<Object>(); 
                pointer.add(lex);
                pointer.add(ent);
                pointer.add(despG);
                despG+=1;
                pointer.add(""); pointer.add(new ArrayList<Object>()); pointer.add(""); pointer.add("");
                TSG.add(pointer);
                existeTSG.put(lex, TSG.size()-1);
            }
            pos = !existe ? TSG.size()-1 : pos;
            boolean tabla = existeTSG.containsKey(lex);
            String tipo = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(1) : (String)((ArrayList<Object>)TSL.get(pos)).get(1);
            String[] q = Q().split(" ");
            if(tipo.equals(fun)){
                if(q[0].equals(vac)){gestorErrores("AnSem", 26, "", ""); return err;}
                else{
                    String numParam = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(3) : (String)((ArrayList<Object>)TSL.get(pos)).get(3);
                    ArrayList<String> tipoParam = tabla ? (ArrayList<String>)((ArrayList<Object>)TSG.get(pos)).get(4) : (ArrayList<String>)((ArrayList<Object>)TSL.get(pos)).get(4);
                    String tipoRet = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(5) : (String)((ArrayList<Object>)TSL.get(pos)).get(5);
                    boolean tParam = q[1].equals(numParam);
                    for(int i=2; i<q.length && tParam;i++){tParam = q[i].equals(tipoParam.get(i-2));}
                    if(tParam) return tipoRet;
                    else{gestorErrores("AnSem", 25, "", ""); return err;}
                }
            } else return tipo;
        }// E3 -> cad
        else if(sig_tok.id() == 12){
            parse(34);
            equipara(12);
            return cad;
        }// E3 -> ent
        else if(sig_tok.id() == 11){
            parse(35);
            equipara(11);
            return ent;
        }
        return err;
    }
    private static String EX1(){
        // EX1 -> + E2 EX1
        if(sig_tok.id() == 22){
            parse(27);
            equipara(22);
            String e = E2();
            String ex = EX1();
            if(e.equals(ent) && (ex.equals(ent) || ex.equals(vac))) return ent;
            else{gestorErrores("AnSem", 23, "", ""); return err;}
        }// EX1 -> lambda
        else if(sig_tok.id() == 24 || sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(50);
            return vac;
        }
        else{
            gestorErrores("AnSin", 11, token_decoder(sig_tok.id()), "");
            return err;
        }
    }    
    private static String Q(){
        //Q -> ( L )
        if(sig_tok.id() == 18){
            parse(33);
            equipara(18);
            String l = L();
            equipara(19);
            return l;
        } // Q -> lambda
        else if(sig_tok.id() == 22 || sig_tok.id() == 24 || sig_tok.id() == 16 || sig_tok.id() == 17 || sig_tok.id() == 19){
            parse(32);
            return vac + " 0";
        }
        else{
            gestorErrores("AnSin", 11, token_decoder(sig_tok.id()), "");
            return err;
        }
    }
    private static String F(){
        parse(40);
        // F -> F1 F2 F3
        String f1 = F1();
        String[] f2 =  F2().split(" ");
        if(f2[0].equals(err)) return err;
        insertarNParam(funActual, f2[1]);
        insertarTipoParam(funActual, f2);
        String f3 = F3();
        TSGActiva = true;
        TS.add(TSL);
        if(f1.equals(err) || f2[0].equals(err) || f3.equals(err)){ return err;}
        else return ok;
    }
    private static String F1(){
        parse(41);
        // F1 -> function id H
        if(sig_tok.id() == 6){
            zonaDec = true;
            equipara(6);
            funActual = Integer.parseInt(sig_tok.atributo());
            TSName.add(lexema);
            equipara(13);
            TSGActiva = false;
            TSL = new ArrayList<Object>();
            existeTSL = new HashMap<String,Integer>();
            despL = 0;
            insertarTipoTS(funActual, fun, TSG);
            String h = H();
            insertarTipoRet(funActual, h);
            insertarEtiq(funActual, nuevaEtiq());
            nAmbitos++;
            return ok;
        }
        return err;
    }
    private static String F2(){
        parse(42);
        // F2 -> ( P )
        if(sig_tok.id() == 18){
            equipara(18);
            String p = P();
            equipara(19);
            zonaDec = false;
            if(!p.equals(vac + " 0")){return p;}
            else return "vac 0";
        }
        return err;
    }
    private static String F3(){
        parse(43);
        // F3 -> { C }
        if(sig_tok.id() == 20){
            equipara(20);
            String c = C();
            equipara(21);
            return c;
        }
        return err;
    }
    private static String H(){
        // H -> T
        if(sig_tok.id() == 1 || sig_tok.id() == 2 || sig_tok.id() == 3){
            parse(44);
            String[] t = T().split(" ");
            return t[0];
        } // H -> lambda
        else if(sig_tok.id() == 18){
            parse(51);
            return vac;
        }
        else{
            gestorErrores("AnSin", 20, "", "");
            return err;
        }
    }
    private static String P(){
        // P -> T id P1
        if(sig_tok.id() == 2 || sig_tok.id() == 3 || sig_tok.id() == 1){
            parse(45);
            String[] t = T().split(" ");
            int pos = Integer.parseInt(sig_tok.atributo());
            equipara(13);
            insertarTipoTS(pos, t[0], TSL);
            insertarDespTS(pos, despL, TSL);
            despL+=Integer.parseInt(t[1]);
            String[] p = P1().split(" ");
            if(p[0].equals(vac)) return "tipo_ok 1 "+t[0];
            else{
                p[1]= Integer.toString(Integer.parseInt(p[1])+1);
                return String.join(" ", p) + " "+t[0];
            }
            
        } // P -> lambda
        else if (sig_tok.id() == 19){
            parse(46);
            return vac+ " 0";
        }
        else{
            gestorErrores("AnSin", 13, "", "");
            return err;
        }
    }
    private static String P1(){
        // P1 -> , T id P1
        if(sig_tok.id() == 16){
            parse(47);
            equipara(16);
            String[] t = T().split(" ");
            equipara(13);
            insertarTipoTS(TSL.size()-1, t[0], TSL);
            insertarDespTS(TSL.size()-1, despL, TSL);
            despL+=Integer.parseInt(t[1]);
            String[] p = P1().split(" ");
            if(p[0].equals(vac)) return "tipo_ok 1 "+t[0];
            else{
                p[1]= Integer.toString(Integer.parseInt(p[1])+1);
                return String.join(" ", p) + " "+t[0];
            }
        }// P1 -> lambda
        else if(sig_tok.id() == 19){
            parse(48);
            return vac + " 0";
        }
        else{
            gestorErrores("AnSin", 14, "", "");
            return err;
        }
    }
    private static String C(){
        // C -> B C
        if(sig_tok.id() == 8 || sig_tok.id() == 4 || sig_tok.id() == 5 || sig_tok.id() == 13 || sig_tok.id() == 10 || sig_tok.id() == 9 || sig_tok.id() == 7){
            parse(10);
            String b = B();
            if(b.equals(err)) return err;
            String c = C();
            if(c.equals(vac)) return ok;
            else return c;
            
        }// C -> lambda
        else if(sig_tok.id() == 21){
            parse(11);
            return vac;
        }
        else{
            gestorErrores("AnSin", 15, "", "");
            return err;
        }
    }
    @SuppressWarnings("unchecked")
    private static String S(){
        // S -> id N
        if(sig_tok.id() == 13){
            parse(15);
            int pos = Integer.parseInt(sig_tok.atributo());
            String lex = lexema;
            boolean existe = TSGActiva ? existeTSG.containsKey(lexema) : existeTSL.containsKey(lexema) || existeTSG.containsKey(lexema);
            equipara(13);
            if(!existe){
                ArrayList<Object> pointer = new ArrayList<Object>(); 
                pointer.add(lex);
                pointer.add(ent);
                pointer.add(despG);
                despG+=1;
                pointer.add(""); pointer.add(new ArrayList<Object>()); pointer.add(""); pointer.add("");
                TSG.add(pointer);
                existeTSG.put(lex, TSG.size()-1);
            }
            pos = !existe ? TSG.size()-1 : pos;
            boolean tabla = existeTSG.containsKey(lex);
            String tipo = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(1) : (String)((ArrayList<Object>)TSL.get(pos)).get(1);
            String[] n = N().split(" ");
            if(tipo.equals(fun)){
                String numParam = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(3) : (String)((ArrayList<Object>)TSL.get(pos)).get(3);
                ArrayList<String> tipoParam = tabla ? (ArrayList<String>)((ArrayList<Object>)TSG.get(pos)).get(4) : (ArrayList<String>)((ArrayList<Object>)TSL.get(pos)).get(4);
                boolean tParam = n[2].equals(numParam);
                for(int i=3; i<n.length && tParam;i++){tParam = n[i].equals(tipoParam.get(i-3));}
                if(tParam) return ok;
                else {gestorErrores("AnSem", 25, "", ""); return err;}
            } else if(n[0].equals(tipo)) return ok;
        }// S -> return R ;
        else if(sig_tok.id() == 10){
            parse(19);
            equipara(10);
            String r = R();
            equipara(17);
            if(!TSGActiva){
                String tipoRet = (String)((ArrayList<Object>)TSG.get(funActual)).get(5);
                if(tipoRet.equals(r)) return ok + " " + tipoRet;
                else return err + " " + err;
            }
            return ok + " " + r;
        }// S -> print ( E ) ;
        else if(sig_tok.id() == 9){
            parse(20);
            equipara(9);
            equipara(18);
            String e = E();
            equipara(19);
            equipara(17);
            if(e.equals(cad) || e.equals(ent)) return ok;
            gestorErrores("AnSem", 27, "", "");
            return err;
        }// S -> input ( id ) ;
        else if(sig_tok.id() == 7){
            parse(21);
            equipara(7);
            equipara(18);
            int pos = Integer.parseInt(sig_tok.atributo());
            String lex = lexema;
            boolean existe = TSGActiva ? existeTSG.containsKey(lexema) : existeTSL.containsKey(lexema) || existeTSG.containsKey(lexema);
            equipara(13);
            if(!existe){
                ArrayList<Object> pointer = new ArrayList<Object>(); 
                pointer.add(lex);
                pointer.add(ent);
                pointer.add(despG);
                despG+=1;
                pointer.add(""); pointer.add(new ArrayList<Object>()); pointer.add(""); pointer.add("");
                TSG.add(pointer);
                existeTSG.put(lex, TSG.size()-1);
            }
            pos = !existe ? TSG.size()-1 : pos;
            boolean tabla = existeTSG.containsKey(lex);
            String tipo = tabla ? (String)((ArrayList<Object>)TSG.get(pos)).get(1) : (String)((ArrayList<Object>)TSL.get(pos)).get(1);
            equipara(19);
            equipara(17);
            if(tipo.equals(cad) || tipo.equals(ent)) return ok;
            gestorErrores("AnSem", 27, "", "");
            return err;
        }
        return err;
    }
    private static String N(){
        //N -> ( L ) ;
        if(sig_tok.id() == 18){
            parse(16);
            equipara(18);
            String l = L();
            equipara(19);
            equipara(17);
            return fun + " " + l;
        }//N -> = E ;
        else if(sig_tok.id() == 14){
            parse(17);
            equipara(14);
            String e = E();
            equipara(17);
            return e;
        }//N -> /= E ;
        else if(sig_tok.id() == 15){
            parse(18);
            equipara(15);
            String e = E();
            equipara(17);
            return e;
        }
        return err;
    }
    private static String R(){
        // R -> E
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(22);
            return E(); 
        }// R -> lambda
        else if(sig_tok.id() == 17){
            parse(23);
            return vac;
        }
        else{
            gestorErrores("AnSin", 16, "", "");
            return err;
        }
    }
    private static String L(){
        // L -> E L1
        if(sig_tok.id() == 23 || sig_tok.id() == 18 || sig_tok.id() == 13 || sig_tok.id() == 12 || sig_tok.id() == 11){
            parse(36);
            String e = E();
            String[] l = L1().split(" ");
            if(l[0].equals(vac)) return "tipo_ok 1 "+e;
            else{
                l[1]= Integer.toString(Integer.parseInt(l[1])+1);
                return String.join(" ", l) + " "+e;
            }
        } // L -> lambda
        else if(sig_tok.id() == 19){
            parse(37);
            return vac +" 0";
        }
        else{
            gestorErrores("AnSin", 17, "", "");
            return err;
        }
    }
    private static String L1(){
        // L1 -> , E L1
        if(sig_tok.id() == 16){
            parse(38);
            equipara(16);
            String e = E();
            String[] l = L1().split(" ");
            if(l[0].equals(vac)) return "tipo_ok 1 "+e;
            else{
                l[1]= Integer.toString(Integer.parseInt(l[1])+1);
                return String.join(" ", l) + " "+e;
            }
        } // L1 -> lambda
        else if(sig_tok.id() == 19){
            parse(39);
            return vac;
        }
        else{
            gestorErrores("AnSin", 18, "", "");
            return err;
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
            if(sig_tok == null) { // Si me devuelve un error escribo y exit 1
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

    @SuppressWarnings("unchecked")
    private static void insertarTipoTS(int pos, String tipo, ArrayList<Object> TS){
        ArrayList<Object> atribs = (ArrayList<Object>) TS.get(pos);
        atribs.set(1,tipo);
        TS.set(pos, atribs);
    }
    
    @SuppressWarnings("unchecked")
    private static void insertarDespTS(int pos, int desp, ArrayList<Object> TS){
        ArrayList<Object> atribs = (ArrayList<Object>) TS.get(pos);
        atribs.set(2,desp);
        TS.set(pos, atribs);
    }
    @SuppressWarnings("unchecked")
    private static void insertarNParam(int pos, String nParam){
        ArrayList<Object> atribs = (ArrayList<Object>) TSG.get(pos);
        atribs.set(3,nParam);
        TSG.set(pos, atribs);
    }
    @SuppressWarnings("unchecked")
    private static void insertarTipoParam(int pos, String[] p){
        ArrayList<Object> atribs = (ArrayList<Object>) TSG.get(pos);
        ArrayList<String> params = (ArrayList<String>)atribs.get(4);
        for(int i=2; i<p.length; i++){
            params.add(p[i]);
        }
        atribs.set(4,params);
        TSG.set(pos, atribs);
    }
    @SuppressWarnings("unchecked")
    private static void insertarTipoRet(int pos, String ret){
        ArrayList<Object> atribs = (ArrayList<Object>) TSG.get(pos);
        atribs.set(5,ret);
        TSG.set(pos, atribs);
    }
    @SuppressWarnings("unchecked")
    private static void insertarEtiq(int pos, String etiq){
        ArrayList<Object> atribs = (ArrayList<Object>) TSG.get(pos);
        atribs.set(6,etiq);
        TSG.set(pos, atribs);
    }
    private static String nuevaEtiq(){
        return "EtFun"+nAmbitos;
    }
    private static void gestorErrores(String analiser, int nError, String token, String tok_obligatorio){
        String msg; int linea = lineaActual;
        if(analiser.equals("AnLex"))
            msg = String.format("Error léxico en linea %s: ", linea);
        else if(analiser.equals("AnSin"))
            msg = String.format("Error sintáctico en linea %s: ", linea);
        else
            msg = String.format("Error semántico en linea %s: ", linea);

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
                case 21: 
                    FErr.write(msg+"El tipo de la expresion no coincide con el de la variable\n"); break;
                case 22: 
                    FErr.write(msg+"Se esperaba un tipo (int, boolean, string)\n"); break;
                case 23: 
                    FErr.write(msg+"Ambos lados de la expresion deben de ser enteros\n"); break;
                case 24: 
                    FErr.write(msg+"La expresion debe ser de tipo logico\n"); break;
                case 25: 
                    FErr.write(msg+"El numero de los parametros y el tipo deben coincidir\n"); break;
                case 26: 
                    FErr.write(msg+"La funcion no tiene ningun valor de retorno que asignar a la variable\n"); break;
                case 27: 
                    FErr.write(msg+"La expresion debe ser de tipo entero o cadena\n"); break;                
            }
        } catch(IOException e) {e.printStackTrace();}
    }
    @SuppressWarnings("unchecked")
    private static void writeTS(){
        ArrayList<Object> atribs;
        String lex;
        ArrayList<Object> tablas;
        int n = 2;
        
        tablas = TSG;
        try {
            FTS.write("TABLA DE SIMBOLOS GLOBAL #1:\n");
            while(!tablas.isEmpty()){
                atribs = (ArrayList<Object>)tablas.remove(0);
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
                            if(!atribs.get(j).equals("")) FTS.write("\t+numParam:\t\t'"+atribs.get(j)+"'\n"); break;
                        case 4:
                            ArrayList<String> tipoParam = (ArrayList<String>) atribs.get(j);
                            if(!tipoParam.isEmpty()){
                                int i=1;
                                for(int k=tipoParam.size()-1; k>=0; k--){
                                    FTS.write("\t+tipoParam"+0+(i)+":\t\t'"+tipoParam.get(k)+"'\n");
                                    i++;
                                }
                            } break;
                        case 5:
                            if(!atribs.get(j).equals("")) FTS.write("\t+TipoRetorno:\t\t'"+atribs.get(j)+"'\n"); break;
                        case 6:
                            if(!atribs.get(j).equals("")) FTS.write("\t+EtiqFuncion:\t\t'"+atribs.get(j)+"'\n"); break;
                    }
                }
                while(tablas.isEmpty() && !TSName.isEmpty() && !TS.isEmpty()){ 
                    FTS.write("\n---------------------------------\n");
                    FTS.write("TABLA DE SIMBOLOS DE "+ TSName.get(0)+" #" + n+ ":\n"); 
                    TSName.remove(0); 
                    tablas = (ArrayList<Object>)TS.remove(0);
                    n++;
                }
            }
        } catch (IOException e) { e.printStackTrace();}
    }
}
