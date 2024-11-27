package com.matteo.AppAPIserver.Server;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.matteo.AppAPIserver.Database.Database;
import com.matteo.AppAPIserver.Tools.Tools;
import com.matteo.AppAPIserver.Types.Exchange;
import com.matteo.AppAPIserver.Types.Resource;
import com.matteo.AppAPIserver.Types.User;
import com.matteo.HTTPServer.server.Server;
import com.matteo.HTTPServer.server.Session;
import com.matteo.HTTPServer.server.SessionVariable;

public class ServerManager {
	Server server;
	
	public ServerManager() throws IOException {
		server = new Server();
		server.startServer();
		registerAPI();
	}
	
	public void registerAPI() {
		server.post("/api/register", (req, res) -> {
			final String formatoData = "dd/MM/yyyy";
			
			Session session = req.getSession();
			session.disableExpiry();
			session.start();
			String nome = req.getRequestParamValue("name");
			String cognome = req.getRequestParamValue("surname");
			String dataDiNascitaStr = req.getRequestParamValue("dateOfBirth");
			String email = req.getRequestParamValue("email");
			String password = req.getRequestParamValue("password");
			
			if(nome != null && cognome != null && dataDiNascitaStr != null && email != null && password != null) {
				nome = nome.trim();
				cognome = cognome.trim();
				dataDiNascitaStr = dataDiNascitaStr.trim();
				email = email.trim();
				
				if(nome.isEmpty()) {
					res.status(400).send("Il nome è vuoto!");
				} else if (cognome.isEmpty()) {
					res.status(400).send("Il cognome è vuoto!");
				} else if(!Tools.isValidDate(dataDiNascitaStr, formatoData)) {
					res.status(400).send("La data di nascita non è nel formato " + formatoData);
				} else if(!Tools.isValidEmail(email)) {
					res.status(400).send("Email invalida!");
				} else if(password.isEmpty()) {
					res.status(400).send("La password non è stata fornita");
				} else {
					String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
					LocalDate dateOfBirth = LocalDate.parse(dataDiNascitaStr, DateTimeFormatter.ofPattern(formatoData));
					
					if(Period.between(dateOfBirth, LocalDate.now()).getYears() >= 18) {
						Connection conn = null;
						PreparedStatement stmt = null;
						ResultSet rs = null;
						try {
							conn = Database.connect();
						} catch (SQLException e) {
							res.status(500).send("Errore di connessione al database");
							return;
						}
						
						try {
							stmt = conn.prepareStatement("INSERT INTO Utenti(Nome, Cognome, DataDiNascita, Email, `Password`) VALUES(?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
							stmt.setString(1, nome);
							stmt.setString(2, cognome);
							stmt.setDate(3, Date.valueOf(dateOfBirth));
							stmt.setString(4, email);
							stmt.setString(5, hashedPassword); // Bcrypt o Argon2 per salvare la password hashata?
							if(stmt.executeUpdate() > 0) {
								try {
									rs = stmt.getGeneratedKeys();
								} catch (SQLException e) {
									res.status(500).send("L'utente è stato registrato correttamente, ma si è verificato un errore durante l'ottenimento della chiave.\n Si prega di effettuare il login");
								}
								
								if(rs != null && rs.next()) {
									int userID = rs.getInt(1);
									session.addSessionVariable(new SessionVariable("userID", userID));
									res.status(200).send("OK");
									session.addSessionVariable(new SessionVariable("logged", true));
								}
								
							} else {
								res.status(500).send("Errore di inserimento");
							}
							
						} catch (SQLException e) {
							res.status(500).send("Errore di inserimento");
						} finally {
							if(rs != null) {
								try {
									rs.close();
								} catch (SQLException e) {}
							}
							if(stmt != null) {
								try {
									stmt.close();
								} catch (SQLException e) {}
							}
							
							if(conn != null) {
								try {
									conn.close();
								} catch (SQLException e) {}
							}
						}
					} else {
						res.status(400).send("Devi essere maggiorenne per poterti registrare");
					}
				}
				
			} else {
				res.status(400).send("Dati assenti!");
			}
		});
		
		server.get("/api/getUserInfo", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.disableExpiry();
			SessionVariable logged = session.getSessionVariable("logged");
			if(logged != null && (boolean)logged.getValue() == true) {
				int userID = (Integer)(session.getSessionVariable("userID").getValue());
				Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try {
					conn = Database.connect();
				} catch (SQLException e) {
					res.status(500).send("Errore di connessione al database");
					return;
				}
				
				try {
					stmt = conn.prepareStatement("SELECT Nome, Cognome, DataDiNascita, Email, Conto FROM Utenti WHERE ID = ?;");
					stmt.setInt(1, userID);
					rs = stmt.executeQuery();
					if(rs.next()) {
						String nome = rs.getString(1);
						String cognome = rs.getString(2);
						LocalDate dataDiNascita = rs.getDate(3).toLocalDate();
						String email = rs.getString(4);
						BigDecimal conto = rs.getBigDecimal(5);
						res.status(200).send(new User(nome, cognome, dataDiNascita, email, conto));
					} else {
						res.status(404).send("Utente non trovato!");
					}
				} catch (SQLException e) {
					res.status(500).send("Si è verificato un errore durante il recupero dei dati dal database");
				} finally {
					if(rs != null) {
						try {
							rs.close();
						} catch (SQLException e) {}
					}
					if(stmt != null) {
						try {
							stmt.close();
						} catch (SQLException e) {}
					}
					
					if(conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {}
					}
				}
			} else {
				res.status(401).send("Non è stato ancora effettuato il login");
			}
		});
		
		server.post("/api/login", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.disableExpiry();
			SessionVariable logged = session.getSessionVariable("logged");
			if(logged != null && (boolean)logged.getValue() == true) {
				res.redirect("/api/getUserInfo");
			} else {
				String emailStr = req.getRequestParamValue("email");
				String password = req.getRequestParamValue("password");
				if(emailStr != null && password != null) {
					emailStr = emailStr.trim();
					if(!Tools.isValidEmail(emailStr)) {
						res.status(400).send("Email invalida");
					} else if(password.isEmpty()) {
						res.status(400).send("Password assente");
					} else {
						Connection conn = null;
						PreparedStatement stmt = null;
						ResultSet rs = null;
						try {
							conn = Database.connect();
						} catch (SQLException e) {
							res.status(500).send("Errore di connessione al database");
							return;
						}
						
						try {
							stmt = conn.prepareStatement("SELECT ID, Nome, Cognome, DataDiNascita, Email, Password, Conto FROM Utenti WHERE Email = ?;");
							stmt.setString(1, emailStr);
							rs = stmt.executeQuery();
							if(rs.next()) {
								int id = rs.getInt(1);
								String nome = rs.getString(2);
								String cognome = rs.getString(3);
								LocalDate dataDiNascita = rs.getDate(4).toLocalDate();
								String email = rs.getString(5);
								String dbHashedPassword = rs.getString(6);
								BigDecimal conto = rs.getBigDecimal(7);
								if(BCrypt.checkpw(password, dbHashedPassword)) {
									session.addSessionVariable(new SessionVariable("userID", id));
									res.status(200).send(new User(nome, cognome, dataDiNascita, email, conto));
									session.addSessionVariable(new SessionVariable("logged", true));
								} else {
									res.status(401).send("Email o password errati");
								}
							} else {
								res.status(401).send("Email o password errati");
							}
						} catch (SQLException e) {
							res.status(500).send("Si è verificato un errore durante il recupero dei dati dal database");
						} finally {
							if(rs != null) {
								try {
									rs.close();
								} catch (SQLException e) {}
							}
							if(stmt != null) {
								try {
									stmt.close();
								} catch (SQLException e) {}
							}
							
							if(conn != null) {
								try {
									conn.close();
								} catch (SQLException e) {}
							}
						}
					}
				} else {
					res.status(400).send("Dati assenti!");
				}
			}
		});
		
		server.get("/api/getExchanges", (req, res) -> {
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
			try {
				conn = Database.connect();
			} catch (SQLException e) {
				res.status(500).send("Errore di connessione al database");
				return;
			}
			
			try {
				stmt = conn.prepareStatement("SELECT * FROM Borse;");
				rs = stmt.executeQuery();
				if(rs.next()) {
					ArrayList<Exchange> exchanges = new ArrayList<Exchange>();
					do {
						exchanges.add(new Exchange(rs.getString(1), rs.getString(2)));
					} while (rs.next());
					res.status(200).send(exchanges);
				} else {
					res.status(404).send("Non sono presenti borse nel database");
				}
			} catch (SQLException e) {
				res.status(500).send("Si è verificato un errore durante il recupero dei dati dal database");
			} finally {
				if(rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {}
				}
				if(stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {}
				}
				
				if(conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {}
				}
			}
		});
		
		server.get("/api/getResources", (req, res) -> {
			String MIC = req.getRequestParamValue("MIC");
			if(MIC != null) {
				Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				
				try {
					conn = Database.connect();
				} catch (SQLException e) {
					res.status(500).send("Errore di connessione al database");
					return;
				}
				
				try {
					stmt = conn.prepareStatement("SELECT Risorse.ID, Risorse.Symbol, Risorse.Nome FROM Quotazioni JOIN Risorse ON Quotazioni.ID_Risorsa = Risorse.ID WHERE MIC_Borsa = ?;");
					stmt.setString(1, MIC);
					rs = stmt.executeQuery();
					if(rs.next()) {
						ArrayList<Resource> exchanges = new ArrayList<Resource>();
						do {
							exchanges.add(new Resource(rs.getInt(1), rs.getString(2), rs.getString(3)));
						} while (rs.next());
						res.status(200).send(exchanges);
					} else {
						res.status(404).send("Non sono risorse per la borsa specificata nel database");
					}
				} catch (SQLException e) {
					res.status(500).send("Si è verificato un errore durante il recupero dei dati dal database");
				} finally {
					if(rs != null) {
						try {
							rs.close();
						} catch (SQLException e) {}
					}
					if(stmt != null) {
						try {
							stmt.close();
						} catch (SQLException e) {}
					}
					
					if(conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {}
					}
				}
			} else {
				res.status(400).send("Parametro MIC assente");
			}
		});
		
		server.post("/api/buyAction", (req, res) -> {
			Session session = req.getSession();
			session.disableExpiry();
			SessionVariable userID = session.getSessionVariable("userID");
			if(userID != null) {
				String resourceIDstr = req.getRequestParamValue("resourceID");
				String quantitaStr = req.getRequestParamValue("quantity");
				if(resourceIDstr != null && quantitaStr != null) {
					int resourceID;
					BigDecimal quantita = null;
					try {
						resourceID = Integer.parseInt(resourceIDstr);
					} catch (NumberFormatException e) {
						res.status(400).send("Il parametro resourceID non è un intero");
						return;
					}
					
					try {
						quantita = new BigDecimal(quantitaStr);
					} catch (NumberFormatException e) {
						res.status(400).send("Il parametro quantity non è un BigDecimal");
						return;
					}
					
					if(quantita.compareTo(BigDecimal.ZERO) <= 0) {
						res.status(400).send("Il parametro quantity deve essere positivo");;
						return;
					}
					
					Connection conn = null;
					PreparedStatement stmt = null;
					ResultSet rs = null;
					
					try {
						conn = Database.connect();
					} catch (SQLException e) {
						res.status(500).send("Errore di connessione al database");
						return;
					}
					
					try {
						// TODO aggiugere all'azione esistete o crearla da 0 (come stabilisco ValoreUnitarioAcquisto???)
						stmt = conn.prepareStatement("");
					} catch (SQLException e) {
						return;
					}
					
				} else {
					res.status(400).send("Dati assenti");
				}
			} else {
				res.status(401).send("Non è stato ancora effettuato il login");
			}
		});
	}
}
