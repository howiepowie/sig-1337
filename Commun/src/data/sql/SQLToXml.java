package data.sql;

import geometry.arbreDependance2.ArbreDecision;
import geometry.model.Point;
import geometry.model.Polygone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.google.code.sig_1337.model.xml.route.RouteType;

import data.model.Node;
import data.model.Road;
import data.model.structure.Basin;
import data.model.structure.Building;
import data.model.structure.Forest;
import data.model.structure.Hole;
import data.model.structure.Structure;
import data.sql.graph.DijkstraGraph;

public class SQLToXml {

	/**
	 * Gets all the nodes from the table.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @return The Map&lt;Integer, Node&gt; of the nodes.
	 */
	private static Map<Long, Node> getAllNodes(Connection db) {
		Map<Long, Node> nodes = new HashMap<Long, Node>();
		Statement s;
		try {
			s = db.createStatement();
			/*
			 * SELECT tmp_nodes.id, ST_X(ST_Transform(tmp_nodes.way, 4326)) AS
			 * x, ST_Y(ST_Transform(tmp_nodes.way, 4326)) AS y FROM (SELECT id,
			 * ST_Transform(ST_GeomFromText( CONCAT('POINT(', lon / 100, ' ',
			 * lat / 100, ')') , 900913), 4326) AS way FROM planet_osm_nodes)
			 * tmp_nodes;
			 */
			ResultSet result = s.executeQuery("SELECT tmp_nodes."
					+ SQLHelper.CUSTOM_TABLE_NODES_ID
					// LAT
					+ ", ST_Y(ST_Transform(tmp_nodes."
					+ SQLHelper.CUSTOM_TABLE_NODES_GEOM
					+ ", "
					+ SQLHelper.REAL_SRID
					+ ")) AS "
					+ SQLHelper.CUSTOM_TABLE_NODES_LAT
					// LON
					+ ", ST_X(ST_Transform(tmp_nodes."
					+ SQLHelper.CUSTOM_TABLE_NODES_GEOM + ", "
					+ SQLHelper.REAL_SRID + ")) AS "
					+ SQLHelper.CUSTOM_TABLE_NODES_LON + " FROM (SELECT "
					+ SQLHelper.CUSTOM_TABLE_NODES_ID
					+ ", ST_Transform(ST_GeomFromText(CONCAT('POINT(', "
					+ SQLHelper.CUSTOM_TABLE_NODES_LON + " / 100, ' ', "
					+ SQLHelper.CUSTOM_TABLE_NODES_LAT
					+ " / 100, ')'), 900913), 4326) AS "
					+ SQLHelper.CUSTOM_TABLE_NODES_GEOM + " FROM "
					+ SQLHelper.CUSTOM_TABLE_NODES + ") tmp_nodes");
			while (result.next()) {
				Node tmp = new Node(
						result.getLong(SQLHelper.CUSTOM_TABLE_NODES_ID),
						result.getDouble(SQLHelper.CUSTOM_TABLE_NODES_LAT),
						result.getDouble(SQLHelper.CUSTOM_TABLE_NODES_LON));
				nodes.put(tmp.getId(), tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return nodes;
	}

	/**
	 * Gets all the roads from the table.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @param nodes
	 *            The Map&lt;Integer, Node&gt; of the nodes.
	 * @return The List&lt;Road&gt; of roads.
	 */
	private static List<Road> getAllRoads(Connection db, Map<Long, Node> nodes) {
		List<Road> roads = new ArrayList<Road>();
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_TABLE_ROADS);
			while (result.next()) {
				Road tmp = new Road(
						result.getInt(SQLHelper.CUSTOM_TABLE_ROADS_ID),
						result.getString(SQLHelper.CUSTOM_TABLE_ROADS_NAME),
						SQLHelper.getArray(result
								.getArray(SQLHelper.CUSTOM_TABLE_ROADS_NODES),
								nodes),
						result.getInt(SQLHelper.CUSTOM_TABLE_ROADS_TYPE),
						result.getString(SQLHelper.CUSTOM_TABLE_ROADS_GEOM));
				roads.add(tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return roads;
	}

	/**
	 * Gets all the buildings from the table.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @param nodes
	 *            The Map&lt;Integer, Node&gt; of the nodes.
	 * @return The Map&lt;Integer, Building&gt; of buildings.
	 */
	private static Map<Long, Building> getAllBuildings(Connection db,
			Map<Long, Node> nodes) {
		Map<Long, Building> buildings = new HashMap<Long, Building>();
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES + " WHERE "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES_TYPE + " = '"
					+ Structure.BUILDING + "'");
			while (result.next()) {
				Building tmp = new Building(
						result.getInt(SQLHelper.CUSTOM_TABLE_STRUCTURES_ID),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_NAME),
						SQLHelper.getArray(
								result.getArray(SQLHelper.CUSTOM_TABLE_STRUCTURES_NODES),
								nodes),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_GEOM),
						SQLHelper.getArray(
								result.getArray(SQLHelper.CUSTOM_TABLE_STRUCTURES_NEIGHBORS),
								nodes));
				// Holes will be set in the getAllHoles method
				buildings.put(tmp.getId(), tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return buildings;
	}

	/**
	 * Gets all the forests from the table.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @param nodes
	 *            The Map&lt;Integer, Node&gt; of the nodes.
	 * @return The Map&lt;Integer, Forest&gt; of forests.
	 */
	private static Map<Long, Forest> getAllForests(Connection db,
			Map<Long, Node> nodes) {
		Map<Long, Forest> forests = new HashMap<Long, Forest>();
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES + " WHERE "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES_TYPE + " = '"
					+ Structure.FOREST + "'");
			while (result.next()) {
				Forest tmp = new Forest(
						result.getInt(SQLHelper.CUSTOM_TABLE_STRUCTURES_ID),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_NAME),
						SQLHelper.getArray(
								result.getArray(SQLHelper.CUSTOM_TABLE_STRUCTURES_NODES),
								nodes),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_GEOM));
				// Holes will be set in the getAllHoles method
				forests.put(tmp.getId(), tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return forests;
	}

	/**
	 * Gets all the basins from the table.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @param nodes
	 *            The Map&lt;Integer, Node&gt; of the nodes.
	 * @return The Map&lt;Integer, Basin&gt; of buildings.
	 */
	private static Map<Long, Basin> getAllBasins(Connection db,
			Map<Long, Node> nodes) {
		Map<Long, Basin> basins = new HashMap<Long, Basin>();
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES + " WHERE "
					+ SQLHelper.CUSTOM_TABLE_STRUCTURES_TYPE + " = '"
					+ Structure.BASIN + "'");
			while (result.next()) {
				Basin tmp = new Basin(
						result.getInt(SQLHelper.CUSTOM_TABLE_STRUCTURES_ID),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_NAME),
						SQLHelper.getArray(
								result.getArray(SQLHelper.CUSTOM_TABLE_STRUCTURES_NODES),
								nodes),
						result.getString(SQLHelper.CUSTOM_TABLE_STRUCTURES_GEOM));
				// Holes will be set in the getAllHoles method
				basins.put(tmp.getId(), tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return basins;
	}

	/**
	 * Gets all the holes from the table and adds them to the corresponding
	 * buildings.
	 * 
	 * @param db
	 *            Connection to the SQL Database.
	 * @param nodes
	 *            The Map&lt;Integer, Node&gt; of the nodes.
	 * @param buildings
	 *            The Map&lt;Integer, Building&gt; of the buildings.
	 * @param basins
	 * @param forests
	 */
	private static void getAllHoles(Connection db, Map<Long, Node> nodes,
			Map<Long, Building> buildings, Map<Long, Forest> forests,
			Map<Long, Basin> basins) {
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_TABLE_HOLES);
			while (result.next()) {
				Hole tmp = new Hole(
						result.getInt(SQLHelper.CUSTOM_TABLE_HOLES_ID),
						result.getInt(SQLHelper.CUSTOM_TABLE_HOLES_ID_STRUCTURE),
						SQLHelper.getArray(result
								.getArray(SQLHelper.CUSTOM_TABLE_HOLES_NODES),
								nodes));
				Structure parent;
				// C'est un batiment
				if ((parent = buildings.get(tmp.getIdStructure())) == null)
					// C'est une forêt
					if ((parent = forests.get(tmp.getIdStructure())) == null)
						// C'est un bassin
						if ((parent = basins.get(tmp.getIdStructure())) == null)
							// Ce n'est rien...
							continue;
				// Tout va bien
				parent.addHole(tmp);
			}
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the XML representation of the Point.
	 * 
	 * @param point
	 *            The Point.
	 * @return The XML String of the Point.
	 */
	private static String pointToXML(Point point) {
		return "<point x=\"" + point.x + "\" y=\"" + point.y + "\" />";
	}

	private static String nodeToXML(Node node) {
		return "<point id=\"" + node.getId() + "\" x=\"" + node.getLongitude()
				+ "\" y=\"" + node.getLatitude() + "\" />";
	}

	/**
	 * Returns the XML representation of the Polyedre composed of triangles.
	 * 
	 * @param polygon
	 *            The Polyedre.
	 * @return The XML String of the Polyedre.
	 */
	private static String triangleToXML(Polygone polygon) {
		return "\t\t\t\t\t<triangle>\n" + "\t\t\t\t\t\t"
				+ pointToXML(polygon.points[0]) + "\n" + "\t\t\t\t\t\t"
				+ pointToXML(polygon.points[1]) + "\n" + "\t\t\t\t\t\t"
				+ pointToXML(polygon.points[2]) + "\n"
				+ "\t\t\t\t\t</triangle>\n";
	}

	/**
	 * Returns the bounds of the osm file.
	 * 
	 * @param filename
	 *            The osm file.
	 * @return String of the bounds element.
	 */
	private static String getBounds(String filename) {
		try {
			SAXParserFactory fabrique = SAXParserFactory.newInstance();
			SAXParser parseur = fabrique.newSAXParser();

			File fichier = new File(filename);
			OSMHandler handle = new OSMHandler();
			parseur.parse(fichier, handle);

			return handle.getBounds();
		} catch (ParserConfigurationException e) {
			System.out.println("Erreur de configuration du parseur");
		} catch (SAXException e) {
			System.out.println("Erreur de parsing");
		} catch (IOException e) {
			System.out.println("Erreur d'entrée/sortie");
		}
		return "";
	}

	/**
	 * Return the graph for routing
	 * 
	 * @param db
	 *            Connection to the SQL Database
	 * @return The graph
	 */
	private static Map<Point, ArrayList<Point>> getGraph(Connection db) {
		Map<Point, ArrayList<Point>> map = new HashMap<Point, ArrayList<Point>>();
		Statement s;
		try {
			s = db.createStatement();
			ResultSet result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_GRAPH_SOURCE);
			while (result.next()) {
				Point ori = new Point(
						result.getDouble(SQLHelper.CUSTOM_GRAPH_POINT_X),
						result.getDouble(SQLHelper.CUSTOM_GRAPH_POINT_Y));
				Point voisin = new Point(
						result.getDouble(SQLHelper.CUSTOM_GRAPH_VOISIN_X),
						result.getDouble(SQLHelper.CUSTOM_GRAPH_VOISIN_Y));
				if (map.containsKey(ori))
					map.get(ori).add(voisin);
				else {
					ArrayList<Point> list = new ArrayList<Point>();
					list.add(voisin);
					map.put(ori, list);
				}
			}
			result = s.executeQuery("SELECT * FROM "
					+ SQLHelper.CUSTOM_GRAPH_TARGET);
			while (result.next()) {
				Point ori = new Point(
						result.getDouble(SQLHelper.CUSTOM_GRAPH_POINT_X),
						result.getDouble(SQLHelper.CUSTOM_GRAPH_POINT_Y));
				Point voisin = new Point(
						result.getDouble(SQLHelper.CUSTOM_GRAPH_VOISIN_X),
						result.getDouble(SQLHelper.CUSTOM_GRAPH_VOISIN_Y));
				if (map.containsKey(ori))
					map.get(ori).add(voisin);
				else {
					ArrayList<Point> list = new ArrayList<Point>();
					list.add(voisin);
					map.put(ori, list);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * Returns the XML representation of the Structure.
	 * 
	 * @param structure
	 *            The Structure to XMLize.
	 * @param type
	 * @return The XML String of the Structure.
	 */
	private static String structureToXML(Structure structure, String type) {
		StringBuffer res = new StringBuffer();
		res.append("\t\t\t<" + type + " nom=\"" + structure.getName()
				+ "\" id=\"" + structure.getId() + "\">\n");
		List<Polygone> triangles = Node.toPolygon(structure.getNodes())
				.toTriangles();
		res.append("\t\t\t\t<triangles>\n");
		for (Polygone t : triangles) {
			if (t.points.length == 3 && t.points[0] != null
					&& t.points[1] != null && t.points[2] != null)
				res.append(triangleToXML(t));
		}
		res.append("\t\t\t\t</triangles>\n");
		for (Hole h : structure.getHoles()) {
			List<Polygone> trianglesTrou = Node.toPolygon(h.getNodes())
					.toTriangles();
			res.append("\t\t\t\t<triangles type=\"trou\">\n");
			for (Polygone t : trianglesTrou) {
				res.append(triangleToXML(t));
			}
			res.append("\t\t\t\t</triangles>\n");
		}
		if (type.equals("batiment")) {
			Building b = (Building) structure;
			if (b.getNeighbors() != null && b.getNeighbors().length > 0) {
				res.append("\t\t\t\t<voisins>\n");
				for (Node n : b.getNeighbors()) {
					res.append("\t\t\t\t\t" + nodeToXML(n) + "\n");
				}
				res.append("\t\t\t\t</voisins>\n");
			}
		}
		res.append("\t\t\t</" + type + ">\n");
		return res.toString();
	}

	/**
	 * Generates the XML for the Android application.
	 * 
	 * @param args
	 * 
	 * @param buildings
	 * @param roads
	 * @param forests
	 * @param basins
	 * @param graph
	 * @param isRemote
	 * @param nodes
	 */
	private static String generateXML(String filename, List<Road> roads,
			Map<Long, Basin> basins, Map<Long, Forest> forests,
			Map<Long, Building> buildings, Map<Point, ArrayList<Point>> graph,
			ArbreDecision tree, boolean isRemote) {
		StringBuffer buff = new StringBuffer();
		buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!DOCTYPE sig_1337 SYSTEM \"sig_1337.dtd\">\n"
				+ "<sig_1337>\n\t" + getBounds(filename) + "\n\t<graphics>\n");

		/* Bassins */
		System.out.println("Generating Basins...");
		buff.append("\t\t<bassins>\n");
		for (Basin b : basins.values()) {
			buff.append(structureToXML(b, "bassin"));
		}
		buff.append("\t\t</bassins>\n");
		System.out.println("Basins generated");

		/* Forêts */
		System.out.println("Generating Forests...");
		buff.append("\t\t<forets>\n");
		for (Forest f : forests.values()) {
			buff.append(structureToXML(f, "foret"));
		}
		buff.append("\t\t</forets>\n");
		System.out.println("Forests generated");

		/* Bâtiments */
		System.out.println("Generating Buildings...");
		buff.append("\t\t<batiments>\n");
		for (Building b : buildings.values()) {
			buff.append(structureToXML(b, "batiment"));
		}
		buff.append("\t\t</batiments>\n");
		System.out.println("Buildings generated");

		/* Routes */
		System.out.println("Generating Roads...");
		buff.append("\t\t<routes>\n");
		for (Road r : roads) {
			String type;
			switch (r.getType()) {
			case (Road.CYCLEWAY):
			case (Road.PEDESTRIAN):
			case (Road.PATH):
			case (Road.STEPS):
			case (Road.FOOTWAY):
				type = RouteType.Path.getName();
				break;
			case (Road.TRACK):
			case (Road.RESIDENTIAL):
				type = RouteType.Route.getName();
				break;
			case (Road.PRIMARY):
			case (Road.PRIMARY_LINK):
			case (Road.SECONDARY):
			case (Road.SECONDARY_LINK):
			case (Road.TERTIARY):
			case (Road.TERTIARY_LINK):
			case (Road.SERVICE):
			case (Road.UNCLASSIFIED):
			default:
				type = RouteType.Route.getName();
			}
			buff.append("\t\t\t<route"
					+ (type != null ? " type=\"" + type + "\"" : "") + ">\n");
			for (Node n : r.getNodes()) {
				buff.append("\t\t\t\t" + pointToXML(n.toPoint()) + "\n");
			}
			buff.append("\t\t\t</route>\n");
		}
		buff.append("\t\t</routes>\n");
		System.out.println("Roads generated");
		if (!isRemote) {
			System.out.println("Generating graph...");
			buff.append("\t</graphics>\n\t<graph>\n");
			for (Point p : graph.keySet()) {
				buff.append("\t\t<vertex x=\"" + p.x + "\" y=\"" + p.y
						+ "\">\n");
				for (Point voisin : graph.get(p)) {
					buff.append("\t\t\t" + pointToXML(voisin) + "\n");
				}
				buff.append("\t\t</vertex>\n");
			}
			System.out.println("Graph generated");
			System.out.println("Generating search graph...");
			buff.append("\t</graph>\n\t<tree>\n");
			tree.toXML(buff, "\t\t");
			System.out.println("Search graph generated");
			buff.append("\t</tree>\n");
		}
		buff.append("</sig_1337>");

		return buff.toString();
	}

	/**
	 * Gets the data from the PostGIS database and generate the xml for the
	 * Android application.
	 */
	public static String process(String filename, boolean isRemote) {
		Connection connection;

		try {
			/*
			 * Load the JDBC driver and establish a connection.
			 */
			Class.forName(SQLHelper.SQL_DRIVER);
			connection = DriverManager
					.getConnection(SQLHelper.SERVER_URL + SQLHelper.DB_NAME,
							SQLHelper.USERNAME, SQLHelper.PASSWORD);

			System.out.println("Preprocessing before XML...");
			Map<Long, Node> nodes = getAllNodes(connection);
			List<Road> roads = getAllRoads(connection, nodes);
			Map<Long, Building> buildings = getAllBuildings(connection, nodes);
			Map<Long, Forest> forests = getAllForests(connection, nodes);
			Map<Long, Basin> basins = getAllBasins(connection, nodes);
			getAllHoles(connection, nodes, buildings, forests, basins);

			Map<Point, ArrayList<Point>> graph = !isRemote ? getGraph(connection)
					: null;

			ArbreDecision tree = null;
			if (!isRemote) {
				tree = ArbreDecision.create(buildings.values());
			}

			System.out.println(nodes.size() + " nodes.");
			System.out.println(roads.size() + " roads.");
			System.out.println(basins.size() + " basins.");
			System.out.println(forests.size() + " forests.");
			System.out.println(buildings.size() + " buildings.");

			connection.close();

			return generateXML(filename, roads, basins, forests, buildings,
					graph, tree, isRemote);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getGraph() {
		Connection connection;

		try {
			/*
			 * Load the JDBC driver and establish a connection.
			 */
			Class.forName(SQLHelper.SQL_DRIVER);
			connection = DriverManager
					.getConnection(SQLHelper.SERVER_URL + SQLHelper.DB_NAME,
							SQLHelper.USERNAME, SQLHelper.PASSWORD);

			Map<Long, Node> nodes = getAllNodes(connection);
			List<Road> roads = getAllRoads(connection, nodes);
			Map<Long, Building> buildings = getAllBuildings(connection, nodes);

			DijkstraGraph g = new DijkstraGraph(roads);
			for (Node n : g.dijkstra(buildings.get(31139269L),
					buildings.get(39569056L))) {
				System.out.print(n + " ");
			}
			System.out.println();
			System.out.println("Dijkstra graph writen");
			return g.toJSON();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return "{}";
	}

	public static void main(String[] args) {
		Connection connection;
		try {
			BufferedReader f = new BufferedReader(new FileReader(new File(
					"files/graph.json")));
			DijkstraGraph g = new DijkstraGraph(f.readLine());
			f.close();

			/*
			 * Load the JDBC driver and establish a connection.
			 */
			Class.forName(SQLHelper.SQL_DRIVER);
			connection = DriverManager
					.getConnection(SQLHelper.SERVER_URL + SQLHelper.DB_NAME,
							SQLHelper.USERNAME, SQLHelper.PASSWORD);

			Map<Long, Node> nodes = getAllNodes(connection);
			Map<Long, Building> buildings = getAllBuildings(connection, nodes);

			for (Node n : g.dijkstra(buildings.get(31139269L),
					buildings.get(39569056L))) {
				System.out.print(n + " ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
