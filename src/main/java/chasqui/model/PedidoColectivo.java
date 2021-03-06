package chasqui.model;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import chasqui.exceptions.EstadoPedidoIncorrectoException;
import chasqui.exceptions.PedidoDuplicadoEnGCC;
import chasqui.view.composer.Constantes;

/**
 * @author huenu
 * TODO deberia contar con un metodo que le permita chequear que cada usuario 
 * tiene un solo pedido en el pedido colectivo
 */
public class PedidoColectivo implements IPedido{

	private Map<String,Pedido> pedidosIndividuales;
	private Integer id;
	private String estado;
	private Zona zona;
	private Direccion domicilioEntrega;
	private DateTime fechaCreacion;
	private String comentario;
	
	public PedidoColectivo() {
		pedidosIndividuales = new HashMap<String,Pedido>();
		this.fechaCreacion = new DateTime();
		this.estado = Constantes.ESTADO_PEDIDO_ABIERTO;
	}
	
	public Map<String, Pedido> getPedidosIndividuales() {
		return pedidosIndividuales;
	}

	public void setPedidosIndividuales(Map<String, Pedido> pedidosIndividuales) {
		this.pedidosIndividuales = pedidosIndividuales;
	}
	
	public void agregarPedidoIndividual(Pedido nuevoPedido) {		
		if (this.tienePedidoParaCliente(nuevoPedido.getCliente().getEmail())){
			throw new PedidoDuplicadoEnGCC("El usuario: "+ nuevoPedido.getCliente().getEmail() +" ya posee un pedido vigente para este grupo de compras colectivas");
		}
		else{
			nuevoPedido.setPedidoColectivo(this); //Agregado 2017.07.14
			
			this.pedidosIndividuales.put(nuevoPedido.getCliente().getEmail(), nuevoPedido);
		}
	}

	public boolean tienePedidoParaCliente(String usuarioBuscado) {
		return this.pedidosIndividuales.containsKey(usuarioBuscado);
	}	

	public Pedido buscarPedidoParaCliente(String usuarioBuscado) {
		return this.pedidosIndividuales.get(usuarioBuscado);
	}	

	@Override
	public Double getMontoTotal() {
		Double total=0.0;
		for(Pedido pedido: pedidosIndividuales.values()){
			if(pedido.getEstado().equals(Constantes.ESTADO_PEDIDO_CONFIRMADO)){
				total=total+pedido.getMontoActual();
			}
		}			
		return total;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getEstado() {
		return estado;
	}
	
	public void setEstado(String estado) {
		this.estado = estado;
	}
	
	public boolean estaAbierto(){
		return this.estado.equals(Constantes.ESTADO_PEDIDO_ABIERTO);
	}
	
	/*
	 * Workflow
	 *   *--> ABIERTO --> CONFIRMADO --> ENTREGADO
	 *   *--> ABIERTO --> CANCELADO    
	 */
	@Override
	public void confirmarte() throws EstadoPedidoIncorrectoException{
		if (this.estado.equals(Constantes.ESTADO_PEDIDO_ABIERTO)) {
			this.estado = Constantes.ESTADO_PEDIDO_CONFIRMADO;			
		}
		else{
			throw new EstadoPedidoIncorrectoException("El pedido no estaba abierto");
		}
			
	}
	@Override
	public void cancelar() throws EstadoPedidoIncorrectoException{
		if (this.estado.equals(Constantes.ESTADO_PEDIDO_ABIERTO)) {
			this.estado = Constantes.ESTADO_PEDIDO_CANCELADO;			
		}
		else{
			throw new EstadoPedidoIncorrectoException("El pedido no estaba abierto");
		}
			
	}

	@Override
	public void entregarte() throws EstadoPedidoIncorrectoException {
		if (this.estado.equals(Constantes.ESTADO_PEDIDO_CONFIRMADO)) {
			this.estado = Constantes.ESTADO_PEDIDO_ENTREGADO;			
		}
		else{
			throw new EstadoPedidoIncorrectoException("El pedido no estaba confirmado");
		}
	}

	public boolean tienePedidosAbiertos() {
		for (Pedido pedido : pedidosIndividuales.values()) {
			if (pedido.estaAbierto()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean tienePedidos(){
		return !pedidosIndividuales.isEmpty();
	}


	public boolean todosLosPedidosCancelados() {
		for (Pedido pedido : pedidosIndividuales.values()) {
			if (!pedido.estaCancelado()) {
				return false;
			}
		}
		return true;
	}
	
	

	@Override
	public void setZona(Zona zona) {
		this.zona = zona;
	}

	@Override
	public Zona getZona() {
		return this.zona;
	}

	@Override
	public void setDireccionEntrega(Direccion direccion) {
		this.domicilioEntrega = direccion;
	}
	
	@Override
	public Direccion getDireccionEntrega() {
		return domicilioEntrega;
	}

	public DateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(DateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public String getComentario() {
		return comentario;
	}

	public void setComentario(String comentario) {
		this.comentario = comentario;
	}

}
