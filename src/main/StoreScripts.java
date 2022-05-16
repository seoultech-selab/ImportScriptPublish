package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.eclipse.core.runtime.CoreException;

import hk.ust.cse.pishon.esgen.model.Change;
import hk.ust.cse.pishon.esgen.model.EditOp;
import hk.ust.cse.pishon.esgen.model.EditScript;
import hk.ust.cse.pishon.esgen.model.Node;
import model.NodeEdit;
import model.TreeEdit;
import util.ScriptConverter;
import db.DBManager;

public class StoreScripts {
	public static void main(String[] args) {
		String path = "scripts";
		
		DBManager db = null;
		
		try {
			db = new DBManager("db.properties");
			Connection con = db.getConnection();
			
			File f = new File(path);
			File[] fList = f.listFiles();
			PreparedStatement psIns = con.prepareStatement("insert into script_polish ( `group`, `change_id`, `participant_num`, `change_type`, `old_code`, `old_start_line`, `old_end_line`, `old_start_pos`, `new_code`, `new_start_line`, `new_end_line`, `new_start_pos` ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
			

			System.out.println("Total "+fList.length+" Change Files.");
			for (File directory : fList) {
				String group = directory.getName();
				System.out.println("\nCurrent group name is " + group + ".");
				List<File> files = findScriptFiles(directory);
				HashMap<String, Node> nodesMap = readNodes(files);
				
				for (String changeId : nodesMap.keySet()) {
					System.out.println(changeId);
					Node ChangeNode = nodesMap.get(changeId);
					ArrayList<Node> ChangeChildren = (ArrayList)ChangeNode.children;
					for (Node scriptNode : ChangeChildren) {
						String participantNum = (String)scriptNode.value;
						ArrayList<Node> scriptChildren = (ArrayList)scriptNode.children;
						for (Node row : scriptChildren) {
							EditOp editOp = (EditOp)row.value;
							psIns.clearParameters();
							psIns.setString(1, group);
							psIns.setString(2, changeId);
							psIns.setString(3, participantNum);
							psIns.setString(4, editOp.getType());
							psIns.setString(5, editOp.getOldCode());
							
							if (editOp.getOldStartLine() == 0)
								psIns.setNull(6,  Types.INTEGER);
							else
								psIns.setInt(6, editOp.getOldStartLine());
							
							if (editOp.getOldEndLine() == 0)
								psIns.setNull(7,  Types.INTEGER);
							else
								psIns.setInt(7, editOp.getOldEndLine());
							
							if (editOp.getOldStartPos() == 0)
								psIns.setNull(8,  Types.INTEGER);
							else
								psIns.setInt(8, editOp.getOldStartPos());
							
							psIns.setString(9, editOp.getNewCode());
							
							if (editOp.getNewStartLine() == 0)
								psIns.setNull(10,  Types.INTEGER);
							else
								psIns.setInt(10, editOp.getNewStartLine());
							
							if (editOp.getNewEndLine() == 0)
								psIns.setNull(11,  Types.INTEGER);
							else
								psIns.setInt(11, editOp.getNewEndLine());
							
							if (editOp.getNewStartPos() == 0)
								psIns.setNull(12,  Types.INTEGER);
							else
								psIns.setInt(12, editOp.getNewStartPos());
							
							psIns.addBatch();
						}
					}
				}
				psIns.executeBatch();
				psIns.clearBatch();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}
	
	private static HashMap<String, Node> readNodes(List<File> files) {
		HashMap<String, Node> nodes = new HashMap<>();
		FileInputStream fis = null;
		ObjectInputStream in = null;
		for(File f : files){
			try {
				fis = new FileInputStream(f);
				in = new ObjectInputStream(fis);
				nodes = (HashMap)in.readObject();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					if(fis != null)
						fis.close();
					if(in != null)
						in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return nodes;
	}
	
	private static List<File> findScriptFiles(File path) {
		List<File> files = new ArrayList<>();
		if(path.isDirectory()){
			File[] targets = path.listFiles();
			Arrays.sort(targets, (f1, f2)->(f1.getName().compareTo(f2.getName())));
			for(File f : targets){
				if(f.isDirectory()){
					files.addAll(findScriptFiles(f));
				}else if(f.getName().equals("scripts.obj")){
					files.add(f);
				}
			}
		}else{
			if(path.getName().equals("scripts.obj")){
				files.add(path);
			}
		}
		return files;
	}
}
