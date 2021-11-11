/*
  Matriz de Transicion para el Automata Finito Determinista
  Las filas son los estados y las columnas las acciones semanticas
*/
public class MT_AFD{
    private Par[][] mt;
    public MT_AFD(){
        mt = new Par[7][128];  // Una fila por estado y en cada estado puede haber 128 char diferentes
    }
    public Par getValor(int fila, char columna){
        return mt[fila][columna];
    }
    
    public void addValor(int fila, int columna, Par par){
        mt[fila][columna] = par;
    }

    public void print(){
        for(int i=0; i<mt.length; i++){
            System.out.printf("Fila %d >> \n", i);
            for(int j=0; j<mt[i].length; j++){
                if(mt[i][j] != null){
                    System.out.printf("Columna: %d >> %d - %s | ", j,mt[i][j].estado(),mt[i][j].accion());
                }
            }
            System.out.println("");
        }
    }
}