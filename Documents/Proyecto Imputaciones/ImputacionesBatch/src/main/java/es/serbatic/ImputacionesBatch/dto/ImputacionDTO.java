package es.serbatic.ImputacionesBatch.dto;

import java.util.Date;

public class ImputacionDTO {

	private String PEP;
	private int n_personal;
	private Date fecha;
	private int cantidad;
	
	public ImputacionDTO() {
		
	}

	public ImputacionDTO(String pEP, int n_personal, Date fecha, int cantidad) {
		super();
		PEP = pEP;
		this.n_personal = n_personal;
		this.fecha = fecha;
		this.cantidad = cantidad;
	}

	public String getPEP() {
		return PEP;
	}

	public void setPEP(String pEP) {
		PEP = pEP;
	}

	public int getN_personal() {
		return n_personal;
	}

	public void setN_personal(int n_personal) {
		this.n_personal = n_personal;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public int getCantidad() {
		return cantidad;
	}

	public void setCantidad(int cantidad) {
		this.cantidad = cantidad;
	}
}
