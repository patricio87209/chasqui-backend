package chasqui.dao;

import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import chasqui.model.GrupoCC;
import chasqui.model.Usuario;

public interface GrupoDAO {
	public void altaGrupo(GrupoCC grupo);

	public List<GrupoCC> obtenerGruposDeVendedor(final Integer idVendedor);

	public void eliminarGrupoCC(GrupoCC grupo);

	public void guardarGrupo(GrupoCC grupo);

	GrupoCC obtenerGrupoPorId(Integer idGrupoCC);

	public List<GrupoCC> obtenerGruposDelClienteParaVendedor(String email, Integer idVendedor);

	List<GrupoCC> obtenerGruposEnUnArea(Geometry area);

	List<GrupoCC> obtenerGruposDeVendedorCon(Integer idVendedor, Date d, Date h, String estadoSeleccionado);
}
