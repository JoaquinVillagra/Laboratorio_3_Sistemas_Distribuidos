package cl.usach.sd;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Linkable;

public class Initialization implements Control {
	String prefix;
	int idLayer;
	int idTransport;


	public Initialization(String prefix) {
		this.prefix = prefix;
		this.idLayer = Configuration.getPid(prefix + ".protocol");
		this.idTransport = Configuration.getPid(prefix + ".transport");
	}

	/**
	 * Ejecución -  Crear el overlay en el sistema
	 */
	@Override
	public boolean execute() {
		System.out.println("Inicializando Peers y SuperPeers");
		long idNode;
		int sizeCache = Configuration.getInt(prefix + ".initC");
		int sizeDB = Configuration.getInt(prefix + ".initD");
		int[] db;
		int sizeListMsg = 10;
		int m = Configuration.getInt(prefix + ".initM");
		int n = Configuration.getInt(prefix + ".initN");
		int sizeRed = Network.size();
		int sizeDHT  = calculaDHT(sizeRed);
		int[][] cache;
		long[][] dht;
		String[] ip_puerto = new String[sizeRed];
		int[] peers = new int[sizeRed];
		String[] listMsg;
		int peerRecursos = 0;
		ExampleNode node;
		
		//son inicializados los super peers
		for (int i = 0; i < sizeRed; i++) {
			cache = new int[sizeCache][3];
			listMsg = new String[sizeListMsg];
			idNode = ((ExampleNode) Network.get(i)).getID();
			((ExampleNode) Network.get(i)).initAllSuperPeer(cache, idNode, sizeRed, sizeCache, n, m,sizeListMsg, listMsg);
			try {
				ip_puerto[i] = sha1(Integer.toString(((ExampleNode) Network.get(i)).getIp())+Integer.toString(((ExampleNode) Network.get(i)).getPuerto())).substring(0, 6);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		String[] ip_puerto2 =  ordenaNodosEnRed(ip_puerto, sizeRed);
		for(int i = 0; i < sizeRed; i++){
			peers[i] = (int)(Math.random()*(m-n) + n); //se crean la cantidad de peers por nodo
			peerRecursos = peerRecursos + peers[i]; //se obtiene la cantidad de peers total y por lo tanto la de ficheros
			for(int j=0;j<peers[i];j++){ //son creados los peers por red
				node = new ExampleNode(this.prefix);
				Network.add(node);
			}
		}
		
		//Creación de Red tipo anillo
		int p = -1;
		int a = -1;
		for(int i = 0; i<sizeRed; i++){
			for(int j = 0; j<sizeRed; j++){
				if(ip_puerto2[i].equals(ip_puerto[j])){
					if(p == -1){
						p = j;
					}else if(i==1){
						((Linkable) Network.get(p).getProtocol(0)).addNeighbor(Network.get(j));
						((ExampleNode) Network.get(j)).initAntecesor(p, Integer.valueOf(ip_puerto[j], 16), Integer.valueOf(ip_puerto[p], 16));
						a = j;
					}else if(i<sizeRed-1){
						((Linkable) Network.get(a).getProtocol(0)).addNeighbor(Network.get(j));
						((ExampleNode) Network.get(j)).initAntecesor(a, Integer.valueOf(ip_puerto[j], 16), Integer.valueOf(ip_puerto[a], 16));
						a = j;
					}else if(i==sizeRed-1){
						((Linkable) Network.get(a).getProtocol(0)).addNeighbor(Network.get(j));
						((ExampleNode) Network.get(j)).initAntecesor(a, Integer.valueOf(ip_puerto[j], 16), Integer.valueOf(ip_puerto[a], 16));
						((Linkable) Network.get(j).getProtocol(0)).addNeighbor(Network.get(p));
						((ExampleNode) Network.get(p)).initAntecesor(j, Integer.valueOf(ip_puerto[p], 16), Integer.valueOf(ip_puerto[j], 16));
					}
				}
			}
		}
		//Arreglo de recursos disponibles que pueden ser solicitados
		int[] peerRecursosTotal = new int[peerRecursos];
		//Creación de recursos totales asignados a cada nodo.
		peerRecursosTotal = creaRecursosRed(ip_puerto, peers, peerRecursosTotal, sizeRed);
		int j = 0;
		int total = sizeRed;
		int v = 0;
		int[] vecinos;
		int opcion = -1;
		//Creación de conexión entre SuperPeers y otros peers de la Red
		for(int i=0;i<sizeRed;i++){
			v = 0;
			vecinos = crearArrayVecinos(peers[i], total);
			while(v < ((int)peers[i]/2 + 1)){
				opcion = (int)(Math.random()*(peers[i]));
				if(vecinos[opcion] != -1 && vecinos[opcion] != i){
					((Linkable) Network.get(i).getProtocol(0)).addNeighbor(Network.get(vecinos[opcion]));
					vecinos[opcion] = -1;
					v++;
				}
			}
			total =  total + peers[i];
		}
		total = sizeRed;
		int entrar = 0;
		//Agregando conexiones entre distintos Peers
		for(int i=0;i<sizeRed;i++){
			//agregar vecinos
			for(j=total;j<(peers[i]+total);j++){
				v = 0;
				vecinos = crearArrayVecinos(peers[i], total);
				if(j <= ((peers[i]/3 + total))){
					entrar = 1;
				}
				while(v < ((int)peers[i]/2)){
					opcion = (int)(Math.random()*(peers[i]))+1;
					if(opcion < peers[i]){
						if(vecinos[opcion] != -1 && vecinos[opcion] != j){
							((Linkable) Network.get(j).getProtocol(0)).addNeighbor(Network.get(vecinos[opcion]));
							vecinos[opcion] = -1;
							v++;
							if(v > ((int)peers[i]/2 - 2) && entrar == 1){
								entrar = 0;
								v++;
								((Linkable) Network.get(j).getProtocol(0)).addNeighbor(Network.get(i));
							}
						}
					}
				}
			}
			total =  total + peers[i];
		}
		
		// Creación de la base de datos de cada uno de los Peers de la Red. 
		int base = sizeRed;
		int[] opciones;
		int elecciones;
		int n_peer = 0;
		int[] peer_recursos;
		for(int i = 0; i < sizeRed; i++){
			n_peer = 0;
			for(j = base; j < (base+peers[i]); j++){
				idNode = ((ExampleNode) Network.get(j)).getID();
				db = new int[sizeDB];
				elecciones = 0;
				opciones = crearArrayRecursos(peers[i], base-sizeRed, peerRecursosTotal);
				db[elecciones] = opciones[n_peer];
				opciones[n_peer] = -1;
				elecciones++;
				while(elecciones < sizeDB){
					opcion = (int)(Math.random()*(peers[i]));
					if(opciones[opcion] != -1 && opcion != n_peer){
						db[elecciones] = opciones[opcion];
						opciones[opcion] = -1;
						elecciones++;
					}
				}
				n_peer++;
				peer_recursos = subRecursos(i, peerRecursosTotal, peers);
				((ExampleNode) Network.get(j)).initAllPeer(idNode, db, sizeDB, peer_recursos, peers, i); //se inicializa la base de datos del nodo y se le envian todos los recursos que se encuentran en la red disponibles
			}
			
			base =  base + peers[i]; //la base del arreglo es alamacenada
		}
		
		int[][] ip_puerto_num = convertirStringNum(ip_puerto2, ip_puerto);
		
		//se comienza con la inicialización de la dht
		int potencia;
		int suma;
		int posicion;
		for(int i=0;i<sizeRed;i++){
			dht = new long[sizeDHT][3];
			potencia = 1;
			posicion = findPosicion(ip_puerto_num, i, sizeRed);
			for(j=0;j<sizeDHT;j++){
				suma = potencia + posicion;
				if(suma >= sizeRed){
					suma = suma - sizeRed;
				}
				
				dht[j][0] = ip_puerto_num[suma][0];
				dht[j][1] = ((ExampleNode) Network.get((int)ip_puerto_num[suma][1])).getIp();
				dht[j][2] = ((ExampleNode) Network.get((int)ip_puerto_num[suma][1])).getPuerto();
				potencia = potencia*2;
			}
			((ExampleNode) Network.get(i)).initDht(dht, sizeDHT); //se inicializa la dht creada
		}
		
		return true;
	}
	
	private int findPosicion(int[][] ip_puerto_num, int idNode,int sizeRed){
		for(int i=0;i<sizeRed;i++){
			if(ip_puerto_num[i][1] == idNode){
				return i;
			}
		}
		return -1;
	}
	
	private int[] subRecursos(int superPeers, int[] peerRecusosTotal, int[] peer){
		int[] recursos = new int[peerRecusosTotal.length-peer[superPeers]];
		int base = 0;
		int termino;
		int i=0;
		for(i=0;i<superPeers;i++){
			base = base + peer[i];
		}
		termino =  base + peer[i];
		i = 0;
		int j = 0;
		while(j<(peerRecusosTotal.length-peer[superPeers])){
			if(i == base){
				i = termino;
			}
			recursos[j] = peerRecusosTotal[i];
			j++;
			i++;
		}
		return recursos;
	}
	
	private int buscarPosicion(String hash, String[] ip_puerto){
		for(int i=0; i<ip_puerto.length; i++){
			if(hash.equals(ip_puerto[i])){
				return i;
			}
		}
		return -1;
	}
	
	private int[][] convertirStringNum(String[] ip_puerto2, String[] ip_puerto1){
		int[][] n = new int[ip_puerto2.length][2];
		for(int i=0;i<ip_puerto2.length;i++){
			n[i][0] = Integer.valueOf(ip_puerto2[i],16);
			n[i][1] = buscarPosicion(ip_puerto2[i], ip_puerto1);
		}
		return n;
	}
	
	private int[] crearArrayRecursos(int n, int inicio, int[] recursos){
		int[] r = new int[n];
		int j = 0;
		for(int i=inicio;i<inicio+n;i++){
			r[j] = recursos[i];
			j++;
		}
		return r;
	}
	
	private int[] crearArrayVecinos(int vecinos, int inicio){
		int[] v = new int[vecinos];
		for(int i=0;i<vecinos;i++){
			v[i] = inicio + i;
		}
		return v;
	}

	/*
	 	Función que ordena los SuperPeer de menor a mayor.
	 */
	private String[] ordenaNodosEnRed(String[] ip_puerto, int size_red){
	    String aux;
	    int v_ip_puerto  = 0;
	    int v_ip_puerto2 = 0;
	    for(int i=1;i<size_red;i++){
	    	for(int j=0; j<size_red-1; j++){
	    		v_ip_puerto = Integer.valueOf(ip_puerto[j],16);
	    		v_ip_puerto2 = Integer.valueOf(ip_puerto[j+1],16);
	    		if(v_ip_puerto > v_ip_puerto2){
	    			aux = ip_puerto[j+1];
	    			ip_puerto[j+1] = ip_puerto[j];
	    			ip_puerto[j] = aux;
	    		}
	    	}
	    }
	    return ip_puerto;
	}
	
	/*
	 	Función que es utilizada para Hashar el valor numerico de cada nodo de la Red. 
	 */
	private String sha1(String input) throws NoSuchAlgorithmException {
	    MessageDigest mDigest = MessageDigest.getInstance("SHA1");
	    byte[] result = mDigest.digest(input.getBytes());
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < result.length; i++) {
	        sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
	    }  
	    return sb.toString();
	}
	
	/*
		Función que crea los recursos que tendra cada nodo de la red.
	*/
	private int[] creaRecursosRed(String[] ip_puerto, int[] peers, int[] peerRecusosTotal, int sizeRed){
		String dato = "";
		int[] contador = new int[sizeRed];
		int total = 0;
		int v_ip_puerto = 0;
		int v_ip_puerto2 = 0;
		int v_dato = 0;
		for(int x=0;x<sizeRed;x++){
			for(int i=0;true;i++){
				try {
					dato = sha1(Integer.toString(i)).substring(0,6);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				if(x == 0){
					v_ip_puerto = Integer.valueOf(ip_puerto[sizeRed-1],16);
					v_ip_puerto2 = Integer.valueOf(ip_puerto[x],16);
					v_dato = Integer.valueOf(dato,16);
					if(v_ip_puerto < v_dato || v_ip_puerto2 >= v_dato){
						peerRecusosTotal[total] = i;
						contador[x]++;
						total++;
					}
				}else{
					v_ip_puerto = Integer.valueOf(ip_puerto[x],16);
					v_ip_puerto2 = Integer.valueOf(ip_puerto[x-1],16);
					v_dato = Integer.valueOf(dato,16);
					if(v_ip_puerto >= v_dato && v_ip_puerto2 < v_dato){
						peerRecusosTotal[total] = i;
						contador[x]++;
						total++;
					}
				}
				
				if(contador[x] == peers[x]){
					break;
				}
			}
		}
		
		return peerRecusosTotal;
	}
	
	private int calculaDHT(int sizeRed){
		int e = 0;
		int potencia  = 1;
		while(potencia < sizeRed){
			e++;
			potencia = potencia*2;
		}
		return e;
	}
	

	
}
