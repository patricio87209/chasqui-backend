package chasqui.view.composer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.zkoss.spring.SpringUtil;
import org.zkoss.zul.Filedownload;

import chasqui.model.Cliente;
import chasqui.model.Pedido;
import chasqui.model.ProductoPedido;
import chasqui.services.interfaces.ProductoService;

public class XlsExporter {

	private ProductoService productoservice = (ProductoService) SpringUtil.getBean("productoService");
	private Workbook wb = new HSSFWorkbook();
	private Integer page = 0;
	private Map<String, CellStyle> styles = createStyles(wb);

	private Sheet sheet;

	private static final String[] titles = { "PRODUCTOS", "Precio", "Cantidad", "SubTotal", "Código" };
	private static final String[] checkers = { "Baja", "Armado", "Revisado", "Carga", "Entrega" };
	private static final String[] contactinfo = { "Nombre", "Apellido","E-mail", "Telefono", "2do Telefono" };
	private static final String[] contactaddress = { "Calle","Altura","Localidad","Codigo Postal", "Departamento", "Zona", "Comentario" };
	
	public void fullexport(List<Pedido> pedidos) throws Exception {
		for (Pedido p : pedidos) {
			doHeader(p,"Pedido de ");
			doBody(p.getProductosEnPedido().size());
			doContactArea(p);
			loadInfoInTable(p);
			loadInfoInContact(p);
			doDetails();
		}
		showDownload();
		clean();
	}
	
	public void exportColectivos(List<Pedido> pedidos) throws Exception{
		for (Pedido p : pedidos) {
			if(page<1){
				doHeader(p,"Resumen Grupal de ");
			}else{
				doHeader(p,"Pedido de ");
			}
			doBody(p.getProductosEnPedido().size());
			doContactArea(p);
			loadInfoInTable(p);
			loadInfoInContact(p);
			doDetails();
		}
		showDownload();
		clean();
	}

	private void loadInfoInContact(Pedido p) {
		Integer columnindex = 6;
		Cliente c = p.getCliente();
		Integer setPointer= sheet.getLastRowNum();
		setPointer = 2;
		Row row = sheet.getRow(setPointer++);
		Cell cell = row.getCell(columnindex);
		cell.setCellValue(c.getNombre());
		row = sheet.getRow(setPointer++);
		row.getCell(columnindex).setCellValue(c.getApellido());
		row = sheet.getRow(setPointer++);
		row.getCell(columnindex).setCellValue(c.getEmail());
		row = sheet.getRow(setPointer++);
		row.getCell(columnindex).setCellValue(c.getTelefonoFijo());
		row = sheet.getRow(setPointer++);
		if(c.getTelefonoMovil()==null){
			row.getCell(columnindex).setCellValue("No posee");
		}else{
			row.getCell(columnindex).setCellValue(c.getTelefonoMovil());
		}
		if(p.getDireccionEntrega()!=null){
		setPointer ++;
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getDireccionEntrega().getCalle());
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getDireccionEntrega().getAltura());
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getDireccionEntrega().getLocalidad());
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getDireccionEntrega().getCodigoPostal());
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getDireccionEntrega().getDepartamento());
			row = sheet.getRow(setPointer++);
			if(p.getZona() != null){
				row.getCell(columnindex).setCellValue(p.getZona().getNombre());
			}else{
				row.getCell(columnindex).setCellValue("Zona sin asignar");
			}
			row = sheet.getRow(setPointer++);
			row.getCell(columnindex).setCellValue(p.getComentario());
		}
	}

	private void clean() throws IOException {
		page = 0;
		wb.close();
		wb = new HSSFWorkbook();
		styles = createStyles(wb);
		sheet = null;
	}

	private void showDownload() throws IOException {
		File tempFile = File.createTempFile("exportar", "pedido");
		String outputFile = tempFile.getAbsolutePath();
		FileOutputStream out = new FileOutputStream(outputFile);
		wb.write(out);
		Filedownload.save(tempFile, "xls");
		tempFile.deleteOnExit();
		out.close();
	}

	private void doDetails() {
		// Se ajustan los anchos de las celdas, el ancho esta medido en unidades de 1/256
		// del ancho de un caracter
		sheet.setColumnWidth(0, 30 * 256); // 30 caracteres de ancho
		for (int i = 2; i < 4; i++) {
			sheet.setColumnWidth(i, 11 * 256); // 11 caracteres de ancho
		}
		sheet.setColumnWidth(4, 12 * 256); // 12 caracteres de ancho
		sheet.setColumnWidth(5, 18*256);
		sheet.setColumnWidth(6, 26*256);

	}

	private void loadInfoInTable(Pedido pedido) {

		ArrayList<ProductoPedido> productos = new ArrayList<ProductoPedido>(pedido.getProductosEnPedido());
		for (int i = 0; i < productos.size(); i++) {
			Row row = sheet.getRow(2 + i);
			ProductoPedido p = productos.get(i);
			for (int j = 0; j < productos.size(); j++) {
				row.getCell(0).setCellValue(p.getNombreProducto());
				row.getCell(1).setCellValue(p.getPrecio());
				row.getCell(2).setCellValue(p.getCantidad());
				row.getCell(3).setCellValue(p.getCantidad() * p.getPrecio());
				row.getCell(4).setCellValue(productoservice.obtenerVariantePor(p.getIdVariante()).getCodigo().toString());
			}
		}
	}

	private void doHeader(Pedido pedido, String msj) throws Exception {
		this.page = page + 1;
		String titulo = pedido.getCliente().getNombre() + " " + pedido.getCliente().getApellido();
		titulo = msj + titulo;
		sheet = wb.createSheet(page.toString() + "_" + titulo);
		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setLandscape(true);
		sheet.setFitToPage(true);
		sheet.setHorizontallyCenter(true);

		// Fila con el titulo
		Row titleRow = sheet.createRow(0);
		titleRow.setHeightInPoints(45);
		Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue(titulo);
		titleCell.setCellStyle(styles.get("title"));
		sheet.addMergedRegion(CellRangeAddress.valueOf("$A$1:$E$1"));
	}

	private void doBody(Integer size) {
		// Arma la cabecera con los titulos de columnas
		Row headerRow = sheet.createRow(1);
		headerRow.setHeightInPoints(20);
		Cell headerCell;
		for (int i = 0; i < titles.length; i++) {
			headerCell = headerRow.createCell(i);
			headerCell.setCellValue(titles[i]);
			headerCell.setCellStyle(styles.get("header"));
		}

		int rownum = 2;
		for (int i = 0; i < size; i++) {
			Row row = sheet.createRow(rownum++);
			for (int j = 0; j < titles.length; j++) {
				Cell cell = row.createCell(j);
				cell.setCellStyle(styles.get("cell"));
			}
		}

		// Filas con los totales
		Row sumRow = sheet.createRow(rownum++);
		Cell cell;
		cell = sumRow.createCell(0);
		cell = sumRow.createCell(1);
		cell.setCellValue("Total");
		cell.setCellStyle(styles.get("formula"));
		// Marca el campo para el sum.
		Integer endsum = size + 2;
		for (int j = 2; j < 4; j++) {
			cell = sumRow.createCell(j);
			String ref = (char) ('A' + j) + "3:" + (char) ('A' + j) + endsum.toString();
			cell.setCellFormula("SUM(" + ref + ")");
			cell.setCellStyle(styles.get("cell"));
		}

		// Crea la tabla para marcar estado
		sheet.createRow(rownum++);
		for (int j = 0; j < checkers.length; j++) {
			sumRow = sheet.createRow(rownum++);
			cell = sumRow.createCell(0);
			cell = sumRow.createCell(1);
			cell.setCellValue(checkers[j]);
			cell.setCellStyle(styles.get("cell"));
			cell = sumRow.createCell(2);
			cell.setCellStyle(styles.get("cell"));
		}
		

	}
	
	private void doContactArea(Pedido p){
		Cell datacell;
		Integer totalNrows = sheet.getLastRowNum();
		Integer startRow = 2;
		Row aRow = sheet.getRow(startRow -1);
		//titulo
		buildTitle(aRow,"Información de Contacto",5,6,"$F$2:$G$2");	
		//primera seccion
		for(int i= 0; i<contactinfo.length;i++){
			aRow = sheet.getRow(startRow);
			datacell = aRow.createCell(5);
			datacell.setCellValue(contactinfo[i]);
			datacell.setCellStyle(styles.get("cell"));
			datacell = aRow.createCell(6);
			datacell.setCellStyle(styles.get("cell"));
			startRow++;
		}
		//valida si existen mas de 14 filas, si no las crea.
		if(totalNrows < 15){
			for(int i=totalNrows + 1; i<15;i++){
				sheet.createRow(i);
			}
		}
		if(p.getDireccionEntrega()!=null){
			//segunda seccion
			startRow = 8;
			aRow = sheet.getRow(startRow -1);
			//titulo
			buildTitle(aRow,"Detalles de la Dirección",5,6,"$F$8:$G$8");
			for(int i=0; i<contactaddress.length;i++){
				aRow = sheet.getRow(startRow);
				datacell = aRow.createCell(5);
				datacell.setCellValue(contactaddress[i]);
				datacell.setCellStyle(styles.get("cell"));
				datacell = aRow.createCell(6);
				datacell.setCellStyle(styles.get("cell"));
				startRow++;
			}
		}
	}

	private void buildTitle(Row aRow, String msj,Integer celln, Integer celln2,String mergedregion) {
		Cell dataCell= aRow.createCell(celln);
		dataCell.setCellValue(msj);
		aRow.createCell(celln2);
		sheet.addMergedRegion(CellRangeAddress.valueOf(mergedregion));
		dataCell.setCellStyle(styles.get("header2"));
	}

	/**
	 * Seccion para las librerias de stilo
	 */
 	private static Map<String, CellStyle> createStyles(Workbook wb) {
		Map<String, CellStyle> styles = new HashMap<>();
		CellStyle style;
		Font titleFont = wb.createFont();
		titleFont.setFontHeightInPoints((short) 18);
		titleFont.setBold(true);
		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setFont(titleFont);
		styles.put("title", style);

		Font monthFont = wb.createFont();
		monthFont.setFontHeightInPoints((short) 11);
		monthFont.setColor(IndexedColors.WHITE.getIndex());
		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFont(monthFont);
		style.setWrapText(true);
		style.setBorderRight(BorderStyle.THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(BorderStyle.THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(BorderStyle.THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		styles.put("header", style);
		
		Font contactData = wb.createFont();
		contactData.setFontHeightInPoints((short) 11);
		contactData.setColor(IndexedColors.WHITE.getIndex());
		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFont(contactData);
		style.setWrapText(true);
		style.setBorderRight(BorderStyle.THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(BorderStyle.THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(BorderStyle.THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		styles.put("header2", style);

		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setWrapText(true);
		style.setBorderRight(BorderStyle.THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(BorderStyle.THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(BorderStyle.THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		styles.put("cell", style);

		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setWrapText(true);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
		style.setBorderRight(BorderStyle.THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(BorderStyle.THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(BorderStyle.THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		styles.put("formula", style);

		style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
		styles.put("formula_2", style);

		return styles;
	}

}
