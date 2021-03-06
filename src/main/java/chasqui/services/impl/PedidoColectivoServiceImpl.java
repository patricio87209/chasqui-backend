package chasqui.services.impl;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import chasqui.dao.PedidoColectivoDAO;
import chasqui.model.Pedido;
import chasqui.model.PedidoColectivo;
import chasqui.services.interfaces.PedidoColectivoService;


public class PedidoColectivoServiceImpl implements PedidoColectivoService{
	
	@Autowired
	PedidoColectivoDAO pedidoColectivoDao;

	@Override
	public void guardarPedidoColectivo(PedidoColectivo pedidoColectivo) {
		pedidoColectivoDao.guardar(pedidoColectivo);
		
	}
	@Override
	public PedidoColectivo obtenerPedidoColectivoPorID(Integer id){
		return this.pedidoColectivoDao.obtenerPedidoColectivoPorID(id);
	}
	
	@Override
	public Collection<? extends PedidoColectivo> obtenerPedidosColectivosDeVendedorDeGrupo(Integer vendedorid, Integer grupoID, Date d, Date h,
			String estadoSeleccionado, Integer zonaId) {
		return this.pedidoColectivoDao.obtenerPedidosColectivosDeVendedorDeGrupo( vendedorid, grupoID, d, h,estadoSeleccionado, zonaId);
	}

	@Override
	public List<PedidoColectivo> obtenerPedidosColectivosDeGrupo(Integer grupoid) {
		return this.pedidoColectivoDao.obtenerPedidosColectivosDeGrupo(grupoid);
	}

}
