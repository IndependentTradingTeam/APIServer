package com.matteo.AppAPIserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.matteo.AppAPIserver.Database.Database;
import com.matteo.AppAPIserver.Server.ServerManager;

public class App {
	public static int findIndex(String keyName, String[] keys) {
		for(int i = 0; i < keys.length; i++) {
			if(keys[i].equals(keyName)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static void cleanFiles() {
		File folder = new File("D:\\Download\\symbols");
        
        if (folder.exists() && folder.isDirectory()) {
            FilenameFilter csvFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv");
                }
            };
            
            File[] csvFiles = folder.listFiles(csvFilter);
            long numberOfQuery = 0;
            if (csvFiles != null && csvFiles.length > 0) {
                System.out.println("File CSV trovati");
                for (File origFile : csvFiles) {
                	File tempFile = new File(origFile.getPath() + ".tmp");
                	
                	String[] processArgs = {
                			"D:\\Users\\Matteo\\Documents\\Bluetooth\\eraseAsciiExtended.exe",
                			origFile.getPath(),
                			tempFile.getPath()
                	};
                	
                	ProcessBuilder pb = new ProcessBuilder(processArgs);
					try {
						Process process = pb.start();
						int exitCode = process.waitFor();
						System.out.println("PROGRAM EXIT CODE: " + exitCode);
						if(exitCode == 0) {
							origFile.delete();
							tempFile.renameTo(origFile);
						}
					} catch (IOException | InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            }
        }
	}
	public static void buildDB() {
		File folder = new File("D:\\Download\\symbols");
        
        if (folder.exists() && folder.isDirectory()) {
            FilenameFilter csvFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv");
                }
            };
            
            File[] csvFiles = folder.listFiles(csvFilter);
            
            if (csvFiles != null && csvFiles.length > 0) {
                System.out.println("File CSV trovati");
                for (File file : csvFiles) {
                	
                	new Thread(new Runnable() {
                		@Override
                		public void run() {
                			String marketName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            BufferedReader br = null;
                            try {
                            	br = new BufferedReader(new FileReader(file));
                            	String line = br.readLine(); // salto la riga dell'intestazione;
                            	String[] keys = line.split(";");
                            	int indexOfSymbol = findIndex("Symbol", keys);
                            	int indexOfName = findIndex("Name", keys);
                            	if(indexOfSymbol != -1 && indexOfName != -1) {
                            		while((line = br.readLine()) != null) {
                                		keys = line.split(";");
                                		if(Math.max(indexOfName, indexOfName) < keys.length) {
                                			String symbol = keys[indexOfSymbol];
                                    		String name = keys[indexOfName];
                                    		if(!symbol.equals("-")) {
                                        		
                                        		Connection conn = null;
                                        		PreparedStatement stmt = null;
                                        		try {
                                        			conn = Database.connect();
                                        			stmt = conn.prepareStatement("INSERT INTO Risorse(Symbol, Nome) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
                                        			stmt.setString(1, symbol);
                                        			stmt.setString(2, name);
                                        			if(stmt.executeUpdate() > 0) {
                                        				ResultSet rs = stmt.getGeneratedKeys();
                                        				if(rs.next()) {
                                        					int idBorsa = rs.getInt(1);
                                        					stmt.close();
                                        					stmt = conn.prepareStatement("INSERT INTO Quotazioni(MIC_Borsa, ID_Risorsa) VALUES (?, ?)");
                                        					stmt.setString(1, marketName);
                                        					stmt.setInt(2, idBorsa);
                                        					stmt.executeUpdate();
                                        				} else {
                                        					System.err.println("NO KEY WAS FOUND");
                                        				}
                                        				rs.close();
                                        			}
                                        		} catch (SQLException e) {
                                        			e.printStackTrace();
                                        		} finally {
                                        			if(stmt != null) {
                                        				try {
                    										stmt.close();
                    									} catch (SQLException e1) {
                    										// TODO Auto-generated catch block
                    										e1.printStackTrace();
                    									}
                                        			}
                                        			
                                        			if(conn != null) {
                                        				try {
                    										conn.close();
                    									} catch (SQLException e1) {
                    										// TODO Auto-generated catch block
                    										e1.printStackTrace();
                    									}
                                        			}
                                        		}
                                        		
                                    		}
                                		}
                                	}
                            	} else {
                            		System.err.println("Invalid header for " + file.getName());
                            		System.err.println("LINE:");
                            		System.err.println(line);
                            	}
                            } catch (IOException e) {
                            	System.out.println("I/O ERROR on file: " + file.getName());
                            	e.printStackTrace();
                            } finally {
                            	if(br != null) {
                            		try {
        								br.close();
        							} catch (IOException e) {
        								// TODO Auto-generated catch block
        								e.printStackTrace();
        							}
                            	}
                            }
                		}
                	}).start();
                     	
                }
                
            } else {
                System.out.println("Non ci sono file csv");
            }
            
        } else {
            System.out.println("La cartella non esiste o non è una directory");
        }
    }
	
	public static void main(String[] args) {
		//cleanFiles();
		//buildDB();
		
		try {
			new ServerManager();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
