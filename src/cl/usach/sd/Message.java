package cl.usach.sd;

import java.util.ArrayList;

/**
 * Clase la cual vamos a utilizar para enviar datos de un Peer a otro
 */
public class Message {
	private int type;
	private String id; 
	private int recurso; 
	private int hash_recurso; 
	private long origen; 
	private long superPeerOrigen; 
	private long superPeerDestino; 
	private int ttl; 
	private int respuesta; 
	private ArrayList listNode; 
	private int randWalk; 
	
	public Message(int type) {
		setType(type);
		this.listNode = new ArrayList<>();
	}

	public int getRandWalk() {
		return randWalk;
	}

	public void setRandWalk(int randWalk) {
		this.randWalk = randWalk;
	}

	public int sizeListNode(){
		return this.listNode.size();
	}
	
	public ArrayList getListNode() {
		return listNode;
	}

	public void setListNode(ArrayList listNode) {
		this.listNode = listNode;
	}
	
	public void addListNode(long idNode){
		this.listNode.add(idNode);
	}
	
	public long getFinishListNode(){
		int size = this.listNode.size();
		return (long)this.listNode.get(size-1);
	}
	
	public void deleteFinishListNode(){
		int size = this.listNode.size();
		this.listNode.remove(size-1);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getRespuesta() {
		return respuesta;
	}

	public void setRespuesta(int respuesta) {
		this.respuesta = respuesta;
	}

	public int getHash_recurso() {
		return hash_recurso;
	}

	public void setHash_recurso(int hash_recurso) {
		this.hash_recurso = hash_recurso;
	}

	public int getRecurso() {
		return recurso;
	}

	public void setRecurso(int recurso) {
		this.recurso = recurso;
	}

	public long getOrigen() {
		return origen;
	}

	public void setOrigen(long origen) {
		this.origen = origen;
	}
	
	public int restTTL(){
		this.ttl--;
		return ttl;
	}
	
	public int getTTL() {
		return ttl;
	}

	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getSuperPeerOrigen() {
		return superPeerOrigen;
	}

	public void setSuperPeerOrigen(long superPeerOrigen) {
		this.superPeerOrigen = superPeerOrigen;
	}

	public long getSuperPeerDestino() {
		return superPeerDestino;
	}

	public void setSuperPeerDestino(long superPeerDestino) {
		this.superPeerDestino = superPeerDestino;
	}
}
