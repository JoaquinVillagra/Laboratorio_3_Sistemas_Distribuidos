package cl.usach.sd;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import peersim.core.GeneralNode;

public class ExampleNode extends GeneralNode {
	/*
		Valores de caché, DHT DB, RED y respectivos tamaños.
		IP como identificador lógico + puerto.
		Registro de antecesor y sucesor en la red. 
	
	*/
	private int[][] cache; 
	private int sizeCache; 
	private long[][] dht; 
	private int sizeDHT;
	private int[] db; 
	private int sizeDB; 
	private int ip; 
	private int puerto; 
	private long idNode; 
	private long sizeRed; 
	private long idNodeSucesor; 
	private long idNodeAntecesor; 
	//¿Es super peer? 1 si lo es, caso contrario 0. 
	private int superPeer; 
	//Valores que permitiran generar la Red P2P no estructurada
	private int n; 
	private int m;
	//Recursos disponibles en la red
	private int[] peerRecursosTotal; 
	//Cantidad de peers por super peer
	private int[] peers;
	//Super peer al que pertenezco
	private int mySuperPeer;
	//Antecesor del super peer
	private long antecesor;
	//Valor del superPeer
	private int num_superPeer; 
	//Valor del antecesor del super peer
	private int num_antecesor;
	//Contador de los mensajes enviados 
	private int cont_msg; 
	//Registro de los mensajes que ya han llegado a destino
	private String[] listMsg;
	//Tamaño del historial de mensajes exitosos 
	private int sizeListMsg;
	//Registro a utilizar para determinar el valor referenciado hace más tiempo
	private int pListMsg; 
	
	public ExampleNode(String prefix) {
		super(prefix);
	}
	
	/*
		Set/Get para DB, sizes, DHT y caché.
	*/
	public int[] getDb() { return db; }
	public void setDb(int[] db) { this.db = db;}
	public int getSizeDB() { return sizeDB; }
	public void setSizeDB(int sizeDB) { this.sizeDB = sizeDB; }
	public int[][] getCache() { return cache; }
	public void setCache(int[][] cache) { this.cache = cache; }
	public long[][] getDht() { return dht; }
	public void setDht(long[][] dht) { this.dht = dht; }
	public int getIp() { return ip; }
	public void setIp() { this.ip = (int)idNode; }
	public int getPuerto() { return puerto; }
	public void setPuerto() { this.puerto = (int)(Math.random()*1000 + 3000); }
	public int getSuperPeer() { return superPeer; }
	public int getMySuperPeer() { return mySuperPeer; }

	/*
		Función que inicializa todos los super peer de la Red. 
	*/
	public void initAllSuperPeer(int[][] cache, long idNode, int sizeRed, int sizeCache, int n, int m,int sizeListMsg, String[] listMsg){
		this.cache = cache;
		this.sizeCache = sizeCache;
		this.idNode = idNode;
		this.sizeRed = sizeRed;
		this.n = n;
		this.m = m;
		this.superPeer = 1;
		this.cont_msg = 0;
		this.pListMsg = 0;
		this.sizeListMsg = sizeListMsg;
		this.listMsg = listMsg;
		this.mySuperPeer = -1;
		initCache();
		initListMsg();
		setIp();
		setPuerto();
	}
	
	/*
		Función que Inicializa la DHT del nodo. 
	*/
	public void initDht(long[][] dht, int sizeDHT){
		this.sizeDHT = sizeDHT;
		this.dht = dht;
	}
	
	/*
		Función que setea el respectivo antecesor y el super peer correspondiente para un nodo.
	*/
	public void initAntecesor(long antecesor, int num_superPeer,int num_antecesor){
		this.antecesor = antecesor;
		this.num_antecesor = num_antecesor;
		this.num_superPeer = num_superPeer;
	}
	
	/*
		Incialización de variables comunes para todo Peer, indenpendiente del sector de la red a ocupar. 
	*/
	public void initAllPeer(long idNode, int[] db, int sizeDB, int[] peerRecursosTotal, int[] peers, int mySuperPeer){
		this.idNode = idNode;
		this.db = db;
		this.sizeDB = sizeDB;
		this.superPeer = 0;
		this.peerRecursosTotal = peerRecursosTotal;
		this.peers = peers;
		this.mySuperPeer = mySuperPeer;
		setIp();
		setPuerto();
	}
	
	/*
		Inicialización del listado de mensajes.
	*/
	public void initListMsg(){
		String a;
		for(int i=0;i<this.sizeListMsg;i++){
			this.listMsg[i] = null;
		}
	}
	
	/*
		Función que agrega mensaje al listado de mensajes leidos.
		@param id: Id del mensaje leido. 
	*/
	public void addListMsg(String id){
		for(int i=0;i<sizeListMsg;i++){
			if(this.listMsg[i] == null){
				this.listMsg[i] = id;
				return;
			}
		}
		this.listMsg[this.pListMsg] = id;
		this.pListMsg++;
		if(this.pListMsg == (this.sizeListMsg-1)){
			this.pListMsg = 0;
		}
		
	}
	
	/*
		Busqueda de un mensaje (Se comprueba si este ha llegado)
		@param id: Id del mensaje buscado. 
	*/
	public int findListMsg(String id){
		for(int i=0;i<this.sizeListMsg;i++){
			if(id.equals(this.listMsg[i])){
				return i;
			}
		}
		return -1;
	}
	
	/*
		Función que genera el Id del mensaje
	*/
	public String idMsg(){
		String id = "Message"+Integer.toString((int)this.idNode)+Integer.toString(this.cont_msg);
		this.cont_msg++;
		return id;
	}
	/*
		Función que obtiene el valor numerico del SuperPeer. 
	*/
	public int getNum_superPeer() {
		return num_superPeer;
	}
	
	/*
		Seter del valor numerico del SuperPeer
	*/
	public void setNum_superPeer(int num_superPeer) {
		this.num_superPeer = num_superPeer;
	}
	
	/*
		Función que obtiene el valor numerico del antecesor
	*/
	public int getNum_antecesor() {
		return num_antecesor;
	}
	
	/*
		Seter del valor numerico del antecesor
	*/
	public void setNum_antecesor(int num_antecesor) {
		this.num_antecesor = num_antecesor;
	}
	
	/*
		Función que obtiene un recurso de manera Random a consultar
	*/
	public int getRecursoRandom(){
		int opcion = (int)(Math.random()*this.peerRecursosTotal.length);
		return this.peerRecursosTotal[opcion];
	}
	
	/*
		Función que determina si el recurso lo asocio a mi red o no. 
	*/
	public int almacenoRecurso(int recurso){
		if((int) this.idNode == 0){
			if(this.num_superPeer >= recurso || this.num_antecesor < recurso){
				return 1;
			}
		}else{
			if(this.num_superPeer >= recurso && this.num_antecesor < recurso){
				return 1;
			}
		}
		return 0;
	}
	
	/*
		Se busca si un recurso se encuentra en la base de datos.
		@param recurso: Elemento buscado.
		@return: Retorna el mismo recurso consultado,
		caso contrario retorna -1.
	*/
	public int findDb(int recurso){
		for(int i=0;i<this.sizeDB;i++){
			if(recurso == this.db[i]){
				return this.db[i];
			}
		}
		return -1;
	}
	
	/*
		Función que inicializa la caché. 
	*/
	public void initCache(){
		for(int i=0;i<this.sizeCache;i++){
			this.cache[i][0] = -1;
			this.cache[i][1] = -1;
			this.cache[i][2] = -1;
		}
	}
	
	/*
		Se busca si un recurso se encuentra en cache.
		@param recurso: Elemento buscado.
		@return: Retorna el mismo recurso consultado,
		caso contrario retorna -1.
	*/
	public int findCache(int recurso){
		for(int i=0;i<this.sizeCache;i++){
			if(this.cache[i][0] == recurso){
				this.cache[i][2]++;
				return this.cache[i][1];
			}
		}
		return -1;
	}
	
	/*
		Función que agrega en elemento al caché
		@param recurso: Recurso buscado
		@param valor: valor correspondiente al recurso buscado
	*/
	public void addCache(int recurso, int valor){
		for(int i=0;i<this.sizeCache;i++){
			if(this.cache[i][0] == -1){
				this.cache[i][0] = recurso;
				this.cache[i][1] = valor;
				this.cache[i][2] = 0;
				return;
			}
		}
		int p = 0;
		int min = cache[0][2];
		for(int i=0;i<this.sizeCache;i++){
			if(min > cache[i][2]){
				p = i;
				min = cache[i][2];
			}
		}
		this.cache[p][0] = recurso;
		this.cache[p][1] = valor;
		this.cache[p][2] = 0;
	}
	
	/*
		Función que busca en DHT un recurso X.
		@param recurso: Recurso buscado
		@param id: Id del mensaje
		@return: Nodo más cercano al recurso buscado
	*/
	public long findDHT(int recurso, String id)
	{
		if(findListMsg(id)!=-1){
			return this.antecesor;
		}
		if(this.idNode == this.sizeRed-1){
			if(this.num_superPeer < recurso){
				for(int i=0;i<this.sizeDHT; i++){
					if(this.dht[i][1] == 0){
						return this.dht[i][1];
					}
				}
			}
		}

		int p2 = -1;
		int max = -1;
		for(int i = 0; i< this.sizeDHT; i++){
			if(max < (int)this.dht[i][0]){
				p2 = i;
				max = (int)this.dht[i][0];
			}
		}
		
		int value = max;
		int p = -1;
		for(int i = 0; i< this.sizeDHT; i++){
			if((int)this.dht[i][0] > recurso && this.dht[i][0] <= value){
				p = i;
				value = (int)this.dht[i][0];
			}
		}
		
		if(p == -1){
			return this.dht[p2][1];
		}else{
			return this.dht[p][1];
		}
	}
}
