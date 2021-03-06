package chasqui.services.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import chasqui.dao.GrupoDAO;
import chasqui.dao.UsuarioDAO;
import chasqui.dao.VendedorDAO;
import chasqui.dao.ZonaDAO;
import chasqui.exceptions.ArchivoConFormatoIncorrectoException;
import chasqui.exceptions.ErrorDeParseoDeCoordenadasException;
import chasqui.model.Cliente;
import chasqui.model.Direccion;
import chasqui.model.GrupoCC;
import chasqui.model.Zona;
import chasqui.services.interfaces.GeoService;


public class GeoServiceImpl implements GeoService{
	
	@Autowired ZonaDAO zonaDAO;
	@Autowired VendedorDAO vendedorDAO;
	@Autowired UsuarioDAO usuarioDAO;
	@Autowired GrupoDAO grupoDAO;
	
	// Asume que cada poligono esta definido en una linea, y son todos poligonos.
	// El numeroZona es para todas las zonas la misma, solo es para la instanciacion de zonas, 
	// por la limitacion de WKT no se puede pasar el nombre de la zona en el archivo.
	@Override
	public void crearZonasDesdeArchivoWKT(String absolutePath, Integer idVendedor){
		try (BufferedReader file = new BufferedReader(new FileReader(absolutePath))) {
			String line;
			Integer numeroZona = 1;
			List<Zona> zonas = new ArrayList<Zona>();
			List<String>nombresDeZonasQueSeSolapan= new ArrayList<String>();
			while((line = file.readLine())!=null){
				 parsearAZona(line, ("zona-" + numeroZona.toString()), zonas,nombresDeZonasQueSeSolapan,idVendedor);
				 numeroZona ++;
			}
			guardarZonas(nombresDeZonasQueSeSolapan,zonas,idVendedor);
		} catch (IOException e) {
			new ArchivoConFormatoIncorrectoException("El archivo es incorrecto o no se encuentra en la ruta especificada: /n" + " Path: " + absolutePath);
		}
	}

	//Asume que el archivo geoJson tiene un campo descripcion (tentativo) con el nombre del vendedor.
	//Basado en la respuesta de http://umap.openstreetmap.fr/es/ al exportar.
	@Override
	public void crearZonasDesdeGeoJson(String absolutePath){
		JSONParser parser = new JSONParser();
		try {			
			JSONObject obj = (JSONObject) parser.parse(new FileReader(absolutePath));
            JSONArray jsonObjects = (JSONArray) obj.get("features");
            Object[] jsons = jsonObjects.toArray();
            Map<String,String> zonasEnWKT = new HashMap<String,String>();
            String nombreVendedor = null;
            Integer idVendedor = null;
            for(int i = 0; i<jsons.length; i++){
            	JSONObject jsonval = (JSONObject) jsons[i];     	
            	ArrayList<ArrayList<ArrayList<Double>>> coordenadas = (ArrayList<ArrayList<ArrayList<Double>>>) ((JSONObject) jsonval.get("geometry")).get("coordinates");
            	String nombreZona = (String) ((JSONObject) jsonval.get("properties")).get("name");
            	String tipo = (String) ((JSONObject) jsonval.get("geometry")).get("type");
            	if(nombreVendedor == null){
            		nombreVendedor = (String) ((JSONObject) jsonval.get("properties")).get("description");
            		idVendedor = vendedorDAO.obtenerVendedor(nombreVendedor).getId();
            	};
            	
            	String zonaEnformatoWKT = parsearAWkt(tipo,coordenadas.get(0));
            	zonasEnWKT.put(nombreZona, zonaEnformatoWKT);
            }
           crearZonas(zonasEnWKT,idVendedor);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	//primero latitud luego longitud
	//LONGITUD alto
	//LATITUD largo
	//el email es para discrimiar al cliente que consulta, si no el cliente que consulta
	//tambien es parte de la respuesta. (analizar)
	@Override
	public List<Cliente> obtenerClientesCercanos(String email) throws ParseException{
	   Geometry area = crearAreaSobreCliente(email);
	   return usuarioDAO.obtenerClientesCercanos(area,email);
	}
	
	@Override
	public List<GrupoCC> obtenerGCC_CercanosACliente(String email) throws ParseException{		
		Geometry area = crearAreaSobreCliente(email);
		return grupoDAO.obtenerGruposEnUnArea(area);
	}
	
	/*-------------------------------------------
	 *  		OPERACIONES AUXILIARES
	 *-------------------------------------------*/
	private Geometry crearAreaSobreCliente(String email) throws ParseException{
		Cliente cliente = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(email);
		usuarioDAO.inicializarDirecciones(cliente);
		Direccion dir = cliente.obtenerDireccionPredeterminada();
		return crearArea(dir.getLatitud(),dir.getLongitud(),2.0);
	}
	
	private Geometry crearArea(String latitud, String longitud,Double radiokm) throws ParseException {
		Geometry area;
		Point point = new GeometryFactory().createPoint(new Coordinate(Double.parseDouble(latitud),Double.parseDouble(longitud)));
		double radio = kmsARadio(radiokm);
		area = createCircle(point,radio,32);
		return area;
	}
	
	private double kmsARadio(double km){
		double radio1enkm = 6.949682915284921;
		double radio = 1.0;
		return (radio*km)/radio1enkm;
	} 
	
	private static Geometry createCircle(Point p0, double radius, int nSides){
	  GeometricShapeFactory shape = new GeometricShapeFactory(new GeometryFactory());
	  shape.setCentre(p0.getCoordinate());
	  shape.setSize(radius / 16);
	  shape.setNumPoints(nSides);
	  return shape.createCircle();
	}
	

	//map<nombreZona,Poligono WKT>
	private void crearZonas(Map<String, String> zonasEnWKT, Integer idVendedor){
		List<Zona> zonas = new ArrayList<Zona>();
		List<String>nombresDeZonasQueSeSolapan= new ArrayList<String>();
		Set<String> nombresDeZonas = zonasEnWKT.keySet();
		for(String nombreZona:nombresDeZonas){
			String zonaEnWKT = zonasEnWKT.get(nombreZona);
			parsearAZona(zonaEnWKT, nombreZona, zonas,nombresDeZonasQueSeSolapan,idVendedor);
		}
		guardarZonas(nombresDeZonasQueSeSolapan,zonas,idVendedor);		
	}	

	private String parsearAWkt(String tipo, ArrayList<ArrayList<Double>> arrayList) {
		String wktformat = "";
		wktformat = wktformat + tipo.toUpperCase();
		wktformat = wktformat + "(( ";
		int i = 0;
		int sizecoordenadas = arrayList.size();
		for(ArrayList<Double> coord : arrayList){
			if(sizecoordenadas-1 > i){
			wktformat = wktformat + coord.get(0).toString() + " ";
			wktformat = wktformat + coord.get(1).toString() + ", ";
			}else{
				wktformat = wktformat + coord.get(0).toString() + " ";
				wktformat = wktformat + coord.get(1).toString() + "))";
			}
			i++;
		}
		return wktformat;
	}

	private String mensajeDeErrorZonasSolapadas(List<String> zonasQueSeSolapan) {
		String msj = "Las siguientes zonas se solapan con una o mas zonas: /n ";
		
		for(String nombre : zonasQueSeSolapan){
			msj = msj + nombre +"/n";
		}
		return msj;
	}
	//Asume que la lineaEnWKT es para crear un Polygon
	private void parsearAZona(String lineaEnWKT, String nombreZona,List<Zona> zonas,List<String> nombreZonasSolapadas,Integer idVendeor){
		try {
			Polygon geoArea = (Polygon) new WKTReader().read(lineaEnWKT);
			new DateTime();			
			Zona zona = new Zona(nombreZona,DateTime.now(),"");
			zona.setGeoArea(geoArea);
			zona.setIdVendedor(idVendeor);
			validarQueNoSeSolapa(zona,zonas,nombreZona,nombreZonasSolapadas);
		} catch (ParseException e) {
			new ErrorDeParseoDeCoordenadasException("La coordenada es invalida no corresponde a un poligono, revise si la primer coordenada es tambien la ultima:/n " + lineaEnWKT);
		}
	}
	
	private void validarQueNoSeSolapa(Zona zona, List<Zona> zonas, String nombreZona, List<String> nombreZonasSolapadas) {
		if(!seSolapa(zona, zonas)){
			zonas.add(zona);
		}else{
			nombreZonasSolapadas.add(nombreZona);
		}
	}

	private Boolean seSolapa(Zona zona, List<Zona>zonas){
		Boolean seSolapa = false;
		for(Zona vzona : zonas){
			if(!seSolapa){
				seSolapa = zona.getGeoArea().overlaps(vzona.getGeoArea());
			}
		}
		return seSolapa;
	}
	
	private void guardarZonas(List<String> nombresDeZonasQueSeSolapan, List<Zona> zonas, Integer idVendedor) {
		if(!nombresDeZonasQueSeSolapan.isEmpty()){				
			new ErrorDeParseoDeCoordenadasException(this.mensajeDeErrorZonasSolapadas(nombresDeZonasQueSeSolapan)); 
		}else{
			//TODO:validar si hay zonas con el mismo nombre antes de guardar
			//o generar en el servicio y/o DAO la query que lo haga por zona
			for(Zona zona: zonas){
				zonaDAO.guardar(zona);
			}
		}
		
	}
	
}
