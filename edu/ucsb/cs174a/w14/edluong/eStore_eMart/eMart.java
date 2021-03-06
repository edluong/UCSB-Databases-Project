package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.sql.*;
import java.util.*;

public class eMart implements Runnable{
	
	private static eMart ref = null;
	
	// Members that can be operated on by other threads
	private volatile Queue<MartCmd> cmdQueue;

	/**
	 * Singleton class reference accessor/constructor
	 */
	public static eMart Ref() {
		if(ref==null) ref = new eMart();
		return ref;
	}
	private eMart() {
		cmdQueue = new LinkedList<MartCmd>();
	}
	
	/**
	 * Sends the appropriate callback request to this thread. Will fail if there is already a command waiting to be executed.
	 * @param c: the command to be executed.
	 */
	public void inputCommand(MartCmd c) {
		cmdQueue.add(c);
	}

	/**
	 * Begins the thread and waits for input. When an input exists, it executes the input's command.
	 */
	@Override
	public void run() {
		System.out.println("eMart Controller - Hello World!");
		try {
			System.out.println("eMart Controller - Listening for Input.");
			while (!Thread.currentThread().isInterrupted()) {
	            try {
	            	Thread.sleep(50);
	            	while(!cmdQueue.isEmpty()) {
	            		System.out.println("eMart Controller - Input Event!");
	            		cmdQueue.remove().execute();
	            	}
	            }catch(InterruptedException ex) {break;}
			}
			System.out.println("eMart Controller - Listener Closed");
		} catch (SQLException e) {e.printStackTrace();}	
	}
	
	// ====================================================================================================
	// The Java version of thread callbacks : Class interfaces!
	//		These represent database commands for eMart.
	//		Instantiate one of these bad boys and pass to this thread; execute will be called as specified.
	//
	// Here is the base interface:
	// ====================================================================================================
	private interface MartCmd {
		public void execute() throws SQLException;
	}
	// ====================================================================================================
	// Valid eMart command implementations follow :
	// ====================================================================================================
	/**
	 * Get full catalog. Constructor takes destination only.
	 */
	public static class QueryCatalog implements MartCmd {
		private int dest;
		public QueryCatalog(int d) {dest = d;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT c.iid, c.category, c.manufacturer, c.model, c.warranty, c.price ";
			cmd +=			"FROM Catalog c";
			System.out.println("\tCatalog Query - Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
					break;
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
					eStore.Ref().inputCommand(new eStore.QueryItemQuantity(dest));
				default:
			}
		}
	}
	/**
	 * Search items for search terms specified. Constructor takes destination and search term string.
	 */
	public static class QueryCatalogSearch implements MartCmd {
		private String search;
		private int dest;
		public QueryCatalogSearch(int d, String search) {this.dest = d; this.search=search;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd = 	"SELECT c.iid, c.category, c.manufacturer, c.model, c.warranty, c.price FROM Catalog c ";
			boolean desc = search.contains("D.");
			boolean acc  = search.contains("A.");
			if(desc) {
				cmd += ", Descriptions d ";
				if(acc) cmd+=", Accessories a WHERE c.iid=d.iid AND c.iid=a.iid2 AND ";
				else cmd+="WHERE c.iid=d.iid AND ";
			} else if(acc) cmd+=", Accessories a WHERE c.iid=a.iid2 AND ";
			else cmd+="WHERE ";
			cmd +=			search;
			System.out.println("\tCatalog Query - Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
					break;
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
					eStore.Ref().inputCommand(new eStore.QueryItemQuantity(dest));
				default:
			}
		}
	}
	/**
	 * Hands off a item quantities from eStore to a controller within this domain
	 */
	public static class PushItemQuantity implements MartCmd {
		private ResultSet rs;
		private int dest;
		public PushItemQuantity(int d, ResultSet rs){this.dest=d; this.rs=rs;}
		@Override
		public void execute() throws SQLException {
			System.out.println("\tRelay Stock Quantity");
			// Push result to destination
			switch(dest) {
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetCatalogQuantityData(rs);
				default:
			}
		}
	}
	/**
	 * Fetch the cart of a customer. Constructor takes destination and customer id.
	 */
	public static class QueryCartItems implements MartCmd {
		private String cid;
		private int dest;
		public QueryCartItems(int d, String cid) {this.dest = d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd +=			"FROM OrderItems o, Catalog c ";
			cmd +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
			System.out.println("\tCart Query - Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Get all customer orders.
	 */
	public static class QueryCustOrders implements MartCmd {
		private String cid;
		private int dest;
		public QueryCustOrders(int d, String cid) {this.dest=d;this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd1;
			cmd1 ="SELECT oid, cid, total ";
			cmd1 +="FROM Orders ";
			cmd1 +="WHERE isCart = 0 AND cid = '" + cid + "'";
			
			switch(dest){
			case Database.DEST_CSTMR:
				CustGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
			}
		}
	}
	/**
	 * Search customer orders by oid. STUB
	 */
	public static class QueryCustOrdersOid implements MartCmd {
		private String cid;
		private int oid;
		private int dest;
		public QueryCustOrdersOid(int d, int oid, String cid) {this.dest=d;this.cid=cid;this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd1;
			cmd1 ="SELECT oid, cid, total ";
			cmd1 +="FROM Orders ";
			cmd1 +="WHERE oid="+oid + " AND isCart = 0 AND cid = '" + cid + "'";
			
			switch(dest){
			case Database.DEST_CSTMR:
				CustGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
			}
		}
	}
	/**
	 * Adds/removes cart item specified by iid. Constructor takes destination, customer id, item id, and quantity (negative for removal).
	 */
	public static class UpdItemCart implements MartCmd {
		private String cid, iid;
		private int dest, quantity;
		public UpdItemCart(int d, String cid, String iid, int quantity) {this.dest=d; this.cid=cid; this.iid=iid; this.quantity=quantity;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings. First, get item entry, if any, from order items
			String cmd1;
			cmd1  =  		"SELECT quantity  ";
			cmd1 +=			"FROM OrderItems ";
			cmd1 +=			"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
			ResultSet rs = Database.stmt.executeQuery(cmd1);
			if(rs.next()) {
				System.out.println("\tCart Item Update - Modifying existing entry");
				int i = rs.getInt(1) + quantity;
				if(i<=0){cmd1 = "DELETE FROM OrderItems WHERE iid = " + iid + " AND oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";}
				else {
					cmd1 = 		"UPDATE OrderItems ";
					cmd1+= 		"SET quantity = " + i + " ";
					cmd1+= 		"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
				}
			} else {
				System.out.println("\tCart Item Update - No entry found, inserting");
				cmd1 = 		"INSERT INTO OrderItems ";
				cmd1+= 		"VALUES ((SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1), " + iid + ", (SELECT price from Catalog where iid="+iid+"), " + quantity + ")";
			}
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
			System.out.println("\tCart Item Update - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Removes cart item specified by iid. Constructor takes destination, customer id, and item id.
	 */
	public static class RmItemCart implements MartCmd {
		private String cid, iid;
		private int dest;
		public RmItemCart(int d, String cid, String iid) {this.dest=d; this.cid=cid; this.iid=iid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings - one for deleting and one for updating the cart
			String cmd1 =  	"DELETE FROM OrderItems  ";
			cmd1 +=			"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
			System.out.println("\tCart Item Removal - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Removes all items in cid's cart. Constructor takes destination and customer id.
	 */
	public static class RmAllItemCart implements MartCmd {
		private String cid;
		private int dest;
		public RmAllItemCart(int d, String cid) {this.dest=d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings - one for deleting and one for updating the cart
			String cmd1 =  	"DELETE FROM OrderItems  ";
			cmd1 +=			"WHERE oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Orders WHERE cid = '" + cid + "' AND isCart=1)";
			System.out.println("\tCart Item Removal - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Starts the process of creating an order from the customer's cart. Takes destination and cid
	 */
	public static class AddOrderCart implements MartCmd {
		private String cid;
		private int dest;
		public AddOrderCart(int d, String cid) {this.dest=d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Get customer's cart's oid
			String cmd = "SELECT oid FROM Orders WHERE cid = '"+cid+"' AND isCart=1";
			ResultSet rs = Database.stmt.executeQuery(cmd);
			rs.next(); String oid = rs.getString(1);
			// Ask eStore to check item quantities
			eStore.Ref().inputCommand(new eStore.FullfillOrder(dest, cid, oid));
			//ref.inputCommand(new eMart.AddOrder(dest, cid, oid));
		}
	}
	/**
	 * Creates and finalizes a copy of an order. Takes destination, cid, oid
	 */
	public static class AddOrderCopy implements MartCmd {
		private String cid, oid;
		private int dest;
		public AddOrderCopy(int d, String cid, String oid) {this.dest=d; this.cid=cid; this.oid=oid;}
		@Override
		public void execute() {
			// Ask eStore to check item quantities
			eStore.Ref().inputCommand(new eStore.FullfillOrder(dest, cid, oid));
		}
	}
	/**
	 * Finalize an order that has been fulfilled by the eStore. Takes destination, cid, oid
	 */
	public static class AddOrder implements MartCmd {
		private String cid, oid;
		@SuppressWarnings("unused")
		private int dest;
		public AddOrder(int d, String cid, String oid) {this.dest=d; this.cid=cid; this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Create new order entry, get its ID
			String cmd = 	"SELECT oid_seq.nextval as newID FROM Dual ";
			ResultSet rs = Database.stmt.executeQuery(cmd);
			rs.next(); int newoid = rs.getInt(1);
			cmd =			"INSERT INTO Orders "
					+ "VALUES((SELECT oid FROM Orders WHERE oid='"+newoid+"'), 0, 0, "
					+ "(SELECT cid FROM Customers WHERE cid='"+cid+"'), SYSDATE) ";
			Database.stmt.executeQuery(cmd);
			newoid+=1;
			// Copy all items in source order into dest order
			cmd =	"INSERT INTO OrderItems SELECT o.oid, i.iid, c.price, i.quantity "
					+ "FROM Orders o, OrderItems i, Catalog c WHERE o.oid = "+newoid+" AND i.oid = "+oid+" AND c.iid=i.iid";
			rs = Database.stmt.executeQuery(cmd);
			System.out.println(cmd);
			//while(rs.next()) {System.out.println(rs.getString(1)+" "+rs.getString(2)+" "+rs.getString(3)+" "+rs.getString(4));}
			// Update order total
			cmd = 	"SELECT SUM(i.price) FROM OrderItems i WHERE i.oid="+newoid+" GROUP BY i.iid";
			rs = Database.stmt.executeQuery(cmd);
			float total, disc, ship;
			if(rs.next()) {
				total = rs.getFloat(1);
				cmd = "SELECT s1.value as Disc, s2.value as Ship FROM Customers c, Status s1, Status s2 "
						+ "WHERE c.cid = '"+cid+"' AND s1.status=c.status AND s2.status='Shipping'";
				rs = Database.stmt.executeQuery(cmd);
				rs.next(); 
				disc=rs.getFloat(1); 
				ship=rs.getFloat(2);
				if(total>100f) ship = 0.0f;
				System.out.println(total);
				total = total - disc*total + ship*total;
				System.out.println(total);
			} else total = 0;
			cmd =	"UPDATE Orders ";
			cmd+=	"SET total = "+total+" ";
			cmd+=	"WHERE oid = "+newoid;
			Database.stmt.executeQuery(cmd);
			// If source was a cart, remove all items
			cmd = 	"DELETE FROM OrderItems ";
			cmd+=	"WHERE oid = (SELECT oid FROM Orders WHERE cid='"+cid+"' AND oid="+oid+" AND isCart=1)";
			Database.stmt.executeQuery(cmd);
			// Update order history, customer status
			cmd =	"UPDATE PurHistory p SET "
					+ "p.oid1 = (SELECT oid FROM (SELECT * FROM Orders ORDER BY dop DESC) o WHERE ROWNUM = 1 AND o.cid='"+cid+"'), "
					+ "p.oid2 = (SELECT oid FROM (SELECT * FROM Orders ORDER BY dop DESC) o WHERE ROWNUM = 2 AND o.cid='"+cid+"'), "
					+ "p.oid3 = (SELECT oid FROM (SELECT * FROM Orders ORDER BY dop DESC) o WHERE ROWNUM = 3 AND o.cid='"+cid+"') "
					+ "WHERE p.cid = (SELECT cid FROM Customers WHERE cid = '"+cid+"')";
			Database.stmt.executeQuery(cmd);
			cmd =	"SELECT SUM(o.total), (SELECT COUNT(oid) FROM (SELECT * FROM Orders ORDER BY dop DESC) o WHERE ROWNUM <=3) "
					+ "FROM Orders o, PurHistory p WHERE o.oid=p.oid1 OR o.oid=p.oid2 OR o.oid=p.oid3 GROUP BY o.oid ";
			rs = Database.stmt.executeQuery(cmd);
			System.out.println(cmd);
			int count;
			if(rs.next()) { 
				total = rs.getFloat(1);
				count = rs.getInt(2);
				String newStatus = "Green";
				if(total>500f) newStatus = "Gold";
				else if(total>100f) newStatus = "Silver";
				else if(count<3) newStatus = "New";
				System.out.println(newStatus);
				cmd =	"UPDATE Customers ";
				cmd+=	"SET status = (SELECT s.status FROM Status s WHERE s.status='"+newStatus+"')";
				cmd+=	"WHERE cid='"+cid+"'";
				Database.stmt.executeQuery(cmd);
			}
			// Pass updates to CustGUI
			ref.inputCommand(new eMart.QueryCartItems(Database.DEST_CSTMR, cid));
			ref.inputCommand(new eMart.QueryCustStats(Database.DEST_CSTMR, cid));
			ref.inputCommand(new eMart.QueryCustOrders(Database.DEST_EMART, cid));
		}
	}
	/**
	 * Searches for username and password for login.
	 */
	public static class QueryLogin implements MartCmd {
		private String user, pass;
		private int dest;
		public QueryLogin(int d, String user, String pass) {this.dest = d; this.user=user; this.pass=pass;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT c.cid, c.isAdmin ";
			cmd +=			"FROM Customers c ";
			cmd +=			"WHERE c.cid = '" + user + "' AND c.password = '" + pass +"'";
			System.out.println("\tLogin Query - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetLoginResult(Database.stmt.executeQuery(cmd));
					break;
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetLoginResult(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Retrieves customer status, discount, shipping. Takes destination, cid
	 */
	public static class QueryCustStats implements MartCmd {
		private String cid;
		private int dest;
		public QueryCustStats(int d, String cid) {this.dest=d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT c.status, s.value, s1.value ";
			cmd +=			"FROM Customers c, Status s, Status s1 ";
			cmd +=			"WHERE c.cid = '" + cid + "' AND c.status=s.status AND s1.status='Shipping'";
			System.out.println("\tLogin Query - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCustInfo(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Get all orders (and relevant info). Takes destination
	 */
	public static class QueryOrders implements MartCmd {
		private int dest;
		public QueryOrders(int d) {this.dest=d;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings
			String cmd1 =  	"SELECT o.oid, o.cid, o.total ";
			cmd1 +=			"FROM Orders o ";
			cmd1 +=			"WHERE o.isCart=0 ";
			String cmd2 = 	"SELECT COUNT(oid), SUM(total) FROM Orders";
			System.out.println("\tOrders Query - Command = " + cmd1);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetOrdersResults(Database.stmt.executeQuery(cmd2));
					MngrGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
				default:
			}
		}
	}
	/**
	 * Get orders (and relevant info) specified by search string. Takes destination, search string 
	 */
	public static class QueryOrdersSearch implements MartCmd {
		private String s;
		private int dest;
		public QueryOrdersSearch(int d, String s) {this.dest=d; this.s=s;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings
			String cmd1, cmd2;
			if(s.contains("O.")) {
				cmd1 ="SELECT o.oid, o.cid, o.total FROM Orders o WHERE o.isCart=0 AND "+s+" ";
				cmd2 ="SELECT COUNT(o.oid), SUM(o.total) FROM Orders o WHERE o.isCart=0 AND "+s+" GROUP BY o.oid";
			} else {
				cmd1 ="SELECT o.oid, o.cid, o.total FROM Orders o, OrderItems i WHERE o.isCart=0 AND o.oid=i.iid AND "+s+" ";
				cmd2 ="SELECT SUM(i.quantity), SUM(i.price) FROM OrderItems i, Orders o WHERE o.isCart=0 AND o.oid=i.iid AND "+s+" GROUP BY i.iid";
			}
			System.out.println("\tOrders Search Query - Command = " + cmd1);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetOrdersResults(Database.stmt.executeQuery(cmd2));
					MngrGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
				default:
			}
		}
	}
	/**
	 * Get items and quantities in order. Takes destination, oid
	 */
	public static class QueryOrderItems implements MartCmd {
		private String oid;
		private int dest;
		public QueryOrderItems(int d, String oid) {this.dest=d; this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings
			String cmd = 	"SELECT i.iid, i.quantity, i.price ";
			cmd +=			"FROM OrderItems i ";
			cmd +=			"WHERE i.oid="+oid;
			System.out.println("\tOrder Item Query - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetOrdersOverview(Database.stmt.executeQuery(cmd));
					break;
				case Database.DEST_MANAG:
					MngrGUI.Ref().SetOrdersOverview(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Delete order by oid. Takes destination, oid. Skips orders that are needed for customer status and cart
	 */
	public static class RmOrderOid implements MartCmd {
		private String oid;
		private int dest;
		public RmOrderOid(int d, String oid) {this.dest=d; this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"DELETE FROM Orders ";
			cmd +=			"WHERE isCart=0 AND oid = "+oid+" AND oid = ";
			cmd +=			"(SELECT o.oid FROM Orders o, PurHistory p ";
			cmd +=			"WHERE o.oid!=p.oid1 AND o.oid!=p.oid2 AND o.oid!=p.oid3)";
			System.out.println("\tRemove Order - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_EMART:
					Database.stmt.executeQuery(cmd);
				default:
			}
		}
	}
	/**
	 * Delete all unnecessary orders. Takes destination
	 */
	public static class RmOrders implements MartCmd {
		private int dest;
		public RmOrders(int d) {this.dest=d;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"DELETE FROM Orders ";
			cmd +=			"WHERE isCart=0 AND oid = ";
			cmd +=			"(SELECT o.oid FROM Orders o, PurHistory p ";
			cmd +=			"WHERE o.oid!=p.oid1 AND o.oid!=p.oid2 AND o.oid!=p.oid3)";
			System.out.println("\tRemove All Orders - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_EMART:
					Database.stmt.executeQuery(cmd);
				default:
			}
		}
	}
	/**
	 * Changes the price of iid. Takes destination, iid, and new price
	 */
	public static class UpdCatalogPrice implements MartCmd {
		String iid;
		int dest, price;
		public UpdCatalogPrice(int d, String iid, int price) {this.dest=d; this.iid=iid; this.price=price;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"UPDATE Catalog ";
			cmd += 			"SET price = " + price + " ";
			cmd += 			"WHERE iid = " + iid;
			System.out.println("\tAlter Price of Item - Command = " + cmd);
			// Execute command
			switch(dest) {
				case Database.DEST_EMART:
					Database.stmt.executeQuery(cmd);
				default:
			}
		}
	}
	/**
	 * Tell eStore to restock iid to full. Takes destination, iid
	 */
	public static class AddRestockOrder implements MartCmd {
		private String iid;
		private int dest;
		public AddRestockOrder(int d, String iid) {this.dest=d; this.iid=iid;}
		@Override
		public void execute() {
			switch(dest) {
				case Database.DEST_ESTOR:
					eStore.Ref().inputCommand(new eStore.AddRestockReq(dest, iid));
				default:
			}
		}
	}
	// ====================================================================================================
	// End command implementations.
	// ====================================================================================================
}
