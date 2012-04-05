package NLGService.WYSIWYM.transformer;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;

public class JenaLiteral implements Literal {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3395763926397065036L;
	private String label;
	private URI datatype;

	public JenaLiteral(String label, URI datatype) {
		this.label = label;
		this.datatype = datatype;
	}
	
	public String stringValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean booleanValue() {
		// TODO Auto-generated method stub
		return false;
	}

	public byte byteValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public XMLGregorianCalendar calendarValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public BigDecimal decimalValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public double doubleValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float floatValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public URI getDatatype() {
		return datatype;
	}

	public String getLabel() {
		return label;
	}

	public String getLanguage() {
		// TODO Auto-generated method stub
		return null;
	}

	public int intValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public BigInteger integerValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public long longValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public short shortValue() {
		// TODO Auto-generated method stub
		return 0;
	}

}
