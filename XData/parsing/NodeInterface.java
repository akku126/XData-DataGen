/**
 * 
 */
package parsing;

/**
 * @author mathew
 *
 */
public interface NodeInterface {
	
	public String getAliasName();
	public void setAliasName(String aName);
	
	public NodeInterface clone() throws CloneNotSupportedException;

	
	public String toString();
}
