package chasqui.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import chasqui.aspect.Auditada;
import chasqui.dao.VendedorDAO;
import chasqui.exceptions.VendedorInexistenteException;
import chasqui.model.Vendedor;
import chasqui.services.interfaces.VendedorService;

@Auditada
public class VendedorServiceImpl implements VendedorService{

	@Autowired
	VendedorDAO vendedorDAO;
	
	
	@Override
	public List<Vendedor> obtenerVendedores() {
		return vendedorDAO.obtenerVendedores();
	}


	@Override
	public Vendedor obtenerVendedor(String nombreVendedor) throws VendedorInexistenteException {
		Vendedor v = vendedorDAO.obtenerVendedor(nombreVendedor);
		if(v == null){
			throw new VendedorInexistenteException("No existe el vendedor: "+nombreVendedor );
		}
		return v;
	}
	
	
	public VendedorDAO getVendedorDAO() {
		return vendedorDAO;
	}
	public void setVendedorDAO(VendedorDAO vendedorDAO) {
		this.vendedorDAO = vendedorDAO;
	}	
	
	

}
