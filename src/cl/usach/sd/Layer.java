package cl.usach.sd;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.WireKOut;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class Layer implements Cloneable, EDProtocol {
	private static final String PAR_TRANSPORT = "transport";
	private static String prefix = null;
	private int transportId;
	private int layerId;
	private int ttl; //ttl del mensaje
	private int sizeRed; //tamaño de la red
	private int kRandom; //cantidad de kRandom

	@Override
	public void processEvent(Node myNode, int layerId, Object event) {
		ExampleNode node = ((ExampleNode) myNode);
		Message message = (Message) event;
		
		//¿Es peer o super peer ?
		if(node.getSuperPeer() == 0){
			//Mensaje = 0 -> Parte del camino. 
			if(message.getType() == 0){
				Message m = new Message(11);
				m.setRecurso(node.getRecursoRandom());
				m.setOrigen(node.getID());
				m.setTTL(this.ttl);
				m.setId(node.idMsg());
				String sha1 = "";
				try {
					sha1 = sha1(Integer.toString(m.getRecurso())).substring(0,6);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				int valor = Integer.valueOf(sha1, 16);
				m.setHash_recurso(valor);
				sendmessage(myNode, layerId, m);
			}else if(message.getType() == 1){ //Busqueda de recurso, preguntando en la red global. 
				if(message.getTTL() != 0){
					message.restTTL();
					sendmessage(myNode, layerId, message);
				}
			}else if(message.getType() == 3){ //Ha llegado al SuperPeer correspondiente y este propaga la consulta a su segmento.
				if(message.getTTL() != 0){
					int respuesta = node.findDb(message.getRecurso()); 
					if(respuesta != -1){
						message.setRespuesta(respuesta);
						message.setType(4);
						System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-retorno]"+"El peer "+node.getID()+" tiene el recurso "+message.getRecurso()+" y envia como respuesta el valor "+message.getRespuesta());
						sendmessage(myNode, layerId, message);
					}else{
						message.restTTL();
						sendmessage(myNode, layerId, message);
					}
				}else{
					System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-retorno]"+"El peer "+node.getID()+" recibe mensaje del peer "+message.getOrigen()+" solicitando el recurso "+message.getRecurso()+" con TTL "+message.getTTL()+" por lo tanto el mensaje es ELIMINADO");
				}
			}else if(message.getType() == 6){ //el mensaje es devuelto al peer de origen
				message.setType(7);
				sendmessage(myNode, layerId, message);
			}
		}else{ //Es un superPeer
			if(message.getType() == 0){ 
				sendmessage(myNode, layerId, message);
			}else if(message.getType() == 1){ //Indico donde esta el recurso
				if(node.almacenoRecurso(message.getHash_recurso()) == 1){
					System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" tiene en su red peer to peer el recurso "+message.getRecurso());
				}else{
					if(node.findListMsg(message.getId())==-1){
						int respuesta = node.findCache(message.getRecurso());
						if(respuesta == -1){ 
							System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-ida]"+"El super peer "+node.getID()+" recibio el mensaje del peer "+message.getOrigen()+" solicitando el recurso "+message.getRecurso());
							message.setType(2);
							message.setSuperPeerOrigen(node.getID());
							message.addListNode(node.getID());
							sendmessage(myNode, layerId, message); //Reenvio de trama
						}else{
							message.setType(6);
							message.setRespuesta(respuesta);
							System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" posee el recurso "+message.getRecurso()+" en cache envia como respuesta el valor "+message.getRespuesta());
							sendmessage(myNode, layerId, message);
						}
					}else{
						System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-ida]"+"El super peer "+node.getID()+" ya habia recibido el mensaje con id "+message.getId()+" por lo tanto es ELIMINADO ");
					}
				}
				
			}else if(message.getType() == 2){ //Revisión: Tengo el recurso solicitado dentro de mi red 
				if(node.almacenoRecurso(message.getHash_recurso()) == 1){
					int respuesta = node.findCache(message.getRecurso());
					if(respuesta == -1){
						message.setType(31); //comienza Random-Walks
						message.setSuperPeerDestino(node.getID());
						message.setTTL(this.ttl);
						System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" tiene en su red peer to peer el recurso "+message.getRecurso());
						sendmessage(myNode, layerId, message);
					}else{
						message.setType(5); //1-realizo el camino de retorno normal
						message.setRespuesta(respuesta);
						System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" posee el recurso "+message.getRecurso()+" en cache envia como respuesta el valor "+message.getRespuesta());
						sendmessage(myNode, layerId, message);
					}
				}else{ // si no lo almaceno busco si esta en cache
					int respuesta = node.findCache(message.getRecurso());
					if(respuesta == -1){
						message.addListNode(node.getID());
						sendmessage(myNode, layerId, message); //Reenvio de Trama
					}else{
						message.setType(5);//Camino de retorno normal
						message.setRespuesta(respuesta);
						System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" posee el recurso "+message.getRecurso()+" en cache envia como respuesta el valor "+message.getRespuesta());
						sendmessage(myNode, layerId, message);
					}
				}
			}else if(message.getType() == 3){ 
				if(message.getTTL() != 0){
					message.restTTL();
					sendmessage(myNode, layerId, message);
				}else{
					System.out.println("["+message.getId()+"-"+message.getRandWalk()+"]"+"El peer "+node.getID()+" recibe mensaje del peer "+message.getOrigen()+" solicitando el recurso "+message.getRecurso()+" con TTL "+message.getTTL()+" por lo tanto el mensaje es ELIMINADO");
				}
			}else if(message.getType() == 4){ //el mensaje se comienza a devolver por el mismo camino
				if(node.findListMsg(message.getId())==-1){
					node.addCache(message.getRecurso(), message.getRespuesta());
					message.setType(5);
					node.addListMsg(message.getId());
					System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-retorno]"+"El super peer "+node.getID()+" recibio la respuesta al recurso "+message.getRecurso()+" como no lo tenia en cache lo almacenara");
					//System.out.println(Arrays.toString(message.getListNode().toArray()));
					sendmessage(myNode, layerId, message);
				}else{
					System.out.println("["+message.getId()+"-"+message.getRandWalk()+"-retorno]"+"El super peer "+node.getID()+" ya habia recibido el mensaje de respuesta con id "+message.getId()+" por tanto es ELIMINADO ");
				}
			}else if(message.getType() == 5){ //si el recurso no lo tenia lo almaceno en cache
				node.addCache(message.getRecurso(), message.getRespuesta());
				System.out.println("["+message.getId()+"]"+"El super peer "+node.getID()+" recibio la respuesta al recurso "+message.getRecurso()+" como no lo tenia en cache lo almacenara");
				if(message.sizeListNode() == 0){
					message.setType(6);
				}
				sendmessage(myNode, layerId, message);
			}
		}
		
	}
	
	/*
	 * Funcion encargada del procesamiento de los mensajes
	 * En ella se analizan las acciones a realizar dependiendo el tipo del mensaje enviado. 
	 */

	public void sendmessage(Node currentNode, int layerId, Object message) {
		Message m = (Message) message;
		if(m.getType() == 0){ //el mensaje debe ser reenviado
			int idSendNode = CommonState.r.nextInt(Network.size());
			Node sendNode = Network.get(idSendNode);
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, message, layerId);
		}else if(m.getType() == 11){ //se intenta buscar el super peer con un k random walk
			int j;
			Message m2;
			for(int i=0;i<this.kRandom;i++){
				j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
				m2 = new Message(1);
				m2.setOrigen(m.getOrigen());
				m2.setId(m.getId());
				m2.setRecurso(m.getRecurso());
				m2.setHash_recurso(m.getHash_recurso());
				m2.setTTL(this.ttl);
				m2.setRandWalk(i);
				System.out.println("["+m2.getId()+"-"+i+"-ida]"+"El peer "+currentNode.getID()+" solicita el recurso "+m2.getRecurso()+" como no lo posee pregunta al peer "+((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID()+" con TTL "+m.getTTL());
				((Transport) currentNode.getProtocol(transportId)).send(currentNode, ((Linkable) currentNode.getProtocol(0)).getNeighbor(j), m2, layerId);
			}
		
		}else if(m.getType() == 1){ //se usa k random para encontrar el super peer tan solo se reenvia el mensaje
			int j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
			System.out.println("["+m.getId()+"-"+m.getRandWalk()+"-ida]"+"El peer "+currentNode.getID()+" recibe el mensaje del peer "+m.getOrigen()+" solicitando el recurso "+m.getRecurso()+" como no lo posee pregunta al peer "+((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID()+" con TTL "+m.getTTL());
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, ((Linkable) currentNode.getProtocol(0)).getNeighbor(j), message, layerId);
			
		}else if(m.getType() == 2){ //el super peer recibio el mensaje pero como no esta en su red peer to peer usa su dht
			long n = ((ExampleNode) currentNode).findDHT(m.getHash_recurso(), m.getId());
			Node sendNode = Network.get((int)n);
			((ExampleNode)currentNode).addListMsg(m.getId());
			System.out.println("["+m.getId()+"]"+"El super peer "+((ExampleNode) currentNode).getID()+" recibe el mensaje del peer "+m.getOrigen()+ " que esta buscando el recurso "+m.getRecurso()+ " como no lo almacena es su red peer to peer pregunta al super peer "+ sendNode.getID());
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, message, layerId);
		}else if(m.getType() == 31){ //cuando se llega al super peer que almacena el recurso se usa k random walk
			//primer mensaje
			Message m2;
			int j;
			int id;
			for(int i=0;i<this.kRandom;i++){
				m2 = new Message(3);
				m2.setRecurso(m.getRecurso());
				m2.setId(m.getId());
				m2.setOrigen(m.getOrigen());
				m2.setSuperPeerOrigen(m.getSuperPeerOrigen());
				m2.setSuperPeerDestino(m.getSuperPeerDestino());
				m2.setRespuesta(m.getRespuesta());
				m2.setTTL(this.ttl);
				m2.setListNode(m.getListNode());
				m2.setRandWalk(i);
				
				j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
				id = (int)((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID();
				while(id < this.sizeRed){
					j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
					id = (int)((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID();
				}
				System.out.println("["+m2.getId()+"-"+i+"-retorno]"+"El Super peer "+currentNode.getID()+" recibe mensaje del peer "+m2.getOrigen()+" solicitando el recurso "+m2.getRecurso()+" como no lo alamacena pregunta al peer "+((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID()+" con TTL "+m2.getTTL());
				((Transport) currentNode.getProtocol(transportId)).send(currentNode, ((Linkable) currentNode.getProtocol(0)).getNeighbor(j), m2, layerId);
			}
		
		}else if(m.getType() == 3){//se reenvia el mensaje por la red hasta encontrar el recurso
			//primer mensaje
			int j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
			int id = (int)((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID();
			while(id < this.sizeRed){
				j = (int)(Math.random()*((Linkable) currentNode.getProtocol(0)).degree()-1)+1;
				id = (int)((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID();
			}
			if(currentNode.getID()<sizeRed){
				System.out.println("["+m.getId()+"-"+m.getRandWalk()+"-retorno]"+"El super peer "+currentNode.getID()+" recibe el mensaje del peer "+m.getOrigen()+" solicitando el recurso "+m.getRecurso()+" como no lo almacena pregunta al peer "+((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID()+" con TTL "+m.getTTL());
			}else{
				System.out.println("["+m.getId()+"-"+m.getRandWalk()+"-retorno]"+"El peer "+currentNode.getID()+" recibe el mensaje del peer "+m.getOrigen()+" solicitando el recurso "+m.getRecurso()+" como no lo almacena pregunta al peer "+((Linkable) currentNode.getProtocol(0)).getNeighbor(j).getID()+" con TTL "+m.getTTL());
			}
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, ((Linkable) currentNode.getProtocol(0)).getNeighbor(j), m, layerId);
		}else if(m.getType() == 4){ //se retorna la respuesta al super peer
			Node sendNode = Network.get((int)m.getSuperPeerDestino());
			System.out.println("["+m.getId()+"-"+m.getRandWalk()+"-retorno]"+"El peer "+currentNode.getID()+" retorna la respuesta al super peer "+m.getSuperPeerDestino());
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, message, layerId);
		}else if(m.getType() == 5){ //se comienza a devolver por los super peer y almacenando en cache
			Node sendNode = Network.get((int)m.getFinishListNode());
			m.deleteFinishListNode();
			System.out.println("["+m.getId()+"]"+"El super peer "+currentNode.getID()+" retorna la respuesta al super peer "+sendNode.getID());
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, message, layerId);
		}else if(m.getType() == 6){ //y el utimo super peer retorna la respuesta al peer de origen
			Node sendNode = Network.get((int)m.getOrigen());
			System.out.println("["+m.getId()+"]"+"El super peer "+currentNode.getID()+" retorna la respuesta al peer de origen "+m.getOrigen());
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, message, layerId);
		}else if(m.getType() == 7){ //se termino con el mensaje por lo que es momento de generar otra consulta
			System.out.println("["+m.getId()+"]"+"El peer "+currentNode.getID()+" recibe la respuesta al mensaje en donde solicita el recurso "+m.getRecurso()+" y obtiene como respuesta "+m.getRespuesta());
			Message m2 = new Message(0); 
			int idSendNode = CommonState.r.nextInt(Network.size());
			Node sendNode = Network.get(idSendNode);
			((Transport) currentNode.getProtocol(transportId)).send(currentNode, sendNode, m2, layerId);
		}
		
		return;

	}

	private String sha1(String input) throws NoSuchAlgorithmException {
	    MessageDigest mDigest = MessageDigest.getInstance("SHA1");
	    byte[] result = mDigest.digest(input.getBytes());
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < result.length; i++) {
	        sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
	    }
	        
	    return sb.toString();
	}

	/**
	 * Constructor por defecto de la capa Layer del protocolo construido
	 */
	public Layer(String prefix) {
		Layer.prefix = prefix;
		transportId = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		layerId = transportId + 1;
		this.ttl = Configuration.getInt(prefix + "." +"initT");
		this.sizeRed = Configuration.getInt(prefix + "." +"initR");
		this.kRandom = Configuration.getInt(prefix + "." +"initK");
		
	}

	private Node searchNode(int id) {
		return Network.get(id);
	}

	/*
		Definir Clone() para la replicacion de protocolo en peers
	 */
	public Object clone() {
		Layer dolly = new Layer(Layer.prefix);
		return dolly;
	}
	
}
