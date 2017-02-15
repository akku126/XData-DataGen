/**
 * 
 */
package parsing;

/**
 * @author mathew
 *
 */
public interface NodeInterface extends Comparable<NodeInterface>{
	
	public String getAliasName();
	public void setAliasName(String aName);
	
	public NodeInterface clone() throws CloneNotSupportedException;

	
	@Override
	public String toString();
}
