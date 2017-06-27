package cl.usach.sd;

import java.util.Arrays;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;
import peersim.core.Linkable;

public class Observer implements Control {

	private int layerId;
	private String prefix;
	private int sizeRed;
	private int sizeRedG;
	private int sizeCache;
	private int sizeDB;
	private int sizeDHT;
	private int DHT;
	
	/*
	 * Son inicializadas los datos del observer
	 */
	public Observer(String prefix) {
		this.prefix = prefix;
		this.layerId = Configuration.getPid(prefix + ".protocol");
		this.sizeRedG = Network.size();
		this.sizeRed = Configuration.getInt(prefix + ".initR");
		this.sizeCache = Configuration.getInt(prefix + ".initC");
		this.sizeDB = Configuration.getInt(prefix + ".initD");
		this.sizeDHT = calculaSizeDHT();
	}

	@Override
	public boolean execute() {
		
		System.err.println("");
		System.err.println("\tTamaño Red: "+this.sizeRed);
		System.err.println("\tTamao DB: "+this.sizeDB);
		System.err.println("\tTamaño DHT: "+this.sizeDHT);
		System.err.println("\tTamaño Cache: "+this.sizeCache);
		System.err.println("");
		
		ExampleNode node;
		long id;
		System.err.println("\t**** Información SuperPeers *****");
		String dht;
		String cache;
		String subRed;
		String[] peers;
		String vecinos;
		for(int i = 0; i < this.sizeRed; i++) {
			node = (ExampleNode) Network.get(i);
			id = node.getID();
			dht =  getDHTinStringFormat(node.getDht());
			cache = getCacheInStringFormat(node.getCache());
			subRed = Integer.toString((int)id);
			peers = getConetionsPeersInStringFormat(id);
			vecinos = getVecinosToSuperPeerInStringFormat(id);
			System.err.print("\t\tIP: "+id+"\tPuerto: "+node.getPuerto()+"\tDHT: "+dht+"\tCACHE: "+cache+"\tSubRed: "+subRed+"\tN_peers: "+peers[0]+"\tPeers: "+peers[1]+"\tVecinos: "+vecinos+"\n");
			
		}
		System.err.println();
		System.err.println("\tTamaño total de la red: "+this.sizeRedG);
		System.err.println();
		String db;
		for(int i = 0; i < this.sizeRed; i++) {
			System.err.println("\tINFORMACIÓN SUBRED "+i+": ("+getConetionsPeersInStringFormat((long)i)[0]+" peers + 1 superpeer)");
			for(int j = 0; j < this.sizeRedG; j++){
				node = (ExampleNode) Network.get(j);
				if(i == node.getMySuperPeer()){
					vecinos = getVecinosInStringFormat(node.getID());
					db = getValueDBinString(node.getDb());
					System.err.println("\t\tIP: "+j+"\tVecinos: "+vecinos+"\tDB: "+db);
				}
		
			}
			System.err.println();
		}
		
		return false;
	}

	private String getDHTinStringFormat(long[][] dht){
		String d = "";
		for(int i=0;i<this.sizeDHT;i++){
			d = d + Integer.toString((int)dht[i][1])+",";
		}
		return d.substring(0, d.length()-1);
	}
	
	private String getCacheInStringFormat(int[][] cache){
		String c = "";
		for(int i=0;i<this.sizeCache;i++){
			if((int)cache[i][0] == -1){
				c = c + "[]";
			}else{
				c = c + "["+Integer.toString((int)cache[i][0])+"]";
			}
		}
		return c;
	}
	
	private String[] getConetionsPeersInStringFormat(long id){
		String[] p = new String[2];
		p[0] = "";
		p[1] = "";
		ExampleNode node;
		int c = 0;
		for(int i=0;i<this.sizeRedG;i++){
			node = (ExampleNode) Network.get(i);
			if(id == node.getMySuperPeer()){
				c++;
				p[1] = p[1]+Integer.toString((int)node.getID())+",";	
			}
		}
		p[0] = Integer.toString(c);
		p[1] = p[1].substring(0, p[1].length()-1);
		return p;
	}

	private String getVecinosToSuperPeerInStringFormat(long id){
		String v = "";
		ExampleNode node = (ExampleNode) Network.get((int)id);
		for (int i = 1; i < ((Linkable) node.getProtocol(0)).degree(); i++) {
			v = v + Integer.toString((int)((Linkable) node.getProtocol(0)).getNeighbor(i).getID())+",";
			
		}
		return v.substring(0, v.length()-1);
	}
	
	private String getVecinosInStringFormat(long id){
		String v = "";
		ExampleNode node = (ExampleNode) Network.get((int)id);
		for (int i = 0; i < ((Linkable) node.getProtocol(0)).degree(); i++) {
			v = v + Integer.toString((int)((Linkable) node.getProtocol(0)).getNeighbor(i).getID())+",";
			
		}
		return v.substring(0, v.length()-1);
	}

	private String getValueDBinString(int[] db){
		String d = "";
		for(int i=0;i<db.length;i++){
			d = d + Integer.toString(db[i])+",";
		}
		return d.substring(0, d.length()-1);
	}
	
	private int calculaSizeDHT(){
		int e = 0;
		int potencia  = 1;
		while(potencia < this.sizeRed){
			e++;
			potencia = potencia*2;
		}
		return e;
	}
	
}
