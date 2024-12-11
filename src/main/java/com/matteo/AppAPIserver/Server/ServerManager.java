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
import java.util.Collections;
import java.util.Vector;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.matteo.AppAPIserver.Database.Database;
import com.matteo.AppAPIserver.Tools.Tools;
import com.matteo.AppAPIserver.Tools.ThreadPool.ThreadPool;
import com.matteo.AppAPIserver.Types.Exchange;
import com.matteo.AppAPIserver.Types.Resource;
import com.matteo.AppAPIserver.Types.ResourceValues;
import com.matteo.AppAPIserver.Types.User;
import com.matteo.HTTPServer.server.Server;
import com.matteo.HTTPServer.server.Session;
import com.matteo.HTTPServer.server.SessionVariable;
import com.matteo.JavaFetch.JavaFetch;
import com.matteo.JavaFetch.RequestParam;
import com.matteo.JavaFetch.Tools.Buffer;

public class ServerManager {
	private final String API_KEY = "ct8o3o1r01qtkv5spih0ct8o3o1r01qtkv5spihg";
	private final String API_SERVER_PATH;
	private Server server;
	
	public ServerManager() throws IOException {
		server = new Server();
		server.startServer();
		API_SERVER_PATH = "http://localhost:" + server.serverConfig.getHTTP_Port();
		registerAPI();
	}
	
	private boolean updateBalance(int userID, BigDecimal toSubstractOrAdd) {
		Connection conn = null;
		PreparedStatement stmt = null;
		char sign = toSubstractOrAdd.compareTo(BigDecimal.ZERO) >= 0 ? '-' : '+';
		try {
			conn = Database.connect();
			stmt = conn.prepareStatement("UPDATE Utenti SET Conto=Conto" + sign + toSubstractOrAdd.toString() + " WHERE ID = ?;");
			stmt.setInt(1, userID);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			
		} finally {
			Database.closeConnection(conn, stmt);
		}
		return false;
	}

	private Integer registerOrUpdateAction(int userID, int resourceID, BigDecimal quantita) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int actionID = 0;
		Integer toReturn = null;
		try {
			conn = Database.connect();
			stmt = conn.prepareStatement("SELECT ID FROM Azioni WHERE ID_Utente = ? AND ID_Risorsa = ?;");
			stmt.setInt(1, userID);
			stmt.setInt(2, resourceID);
			rs = stmt.executeQuery();
			if(rs.next()) {
				actionID = rs.getInt(1);
				rs.close();
				stmt.close();

				stmt = conn.prepareStatement("UPDATE Azioni SET Quantità = Quantità + ? WHERE ID = ?;", Statement.RETURN_GENERATED_KEYS);
				stmt.setBigDecimal(1, quantita);
				stmt.setInt(2, actionID);
			} else {
				rs.close();
				stmt.close();

				stmt = conn.prepareStatement("INSERT INTO Azioni(Quantità, ID_Utente, ID_Risorsa) VALUES(?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
				stmt.setBigDecimal(1, quantita);
				stmt.setInt(2, userID);
				stmt.setInt(3, resourceID);
			}

			stmt.executeUpdate();
			rs = stmt.getGeneratedKeys(); // nn worka
			if(rs.next()) {
				toReturn = rs.getInt(1);
			} else {
				System.err.println("no next");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			Database.closeConnection(conn, stmt, rs);
		}

		return toReturn;
	}

	private void deleteAction(int actionID) {
		do{
			Connection conn = null;
			PreparedStatement stmt = null;
			try {
				conn = Database.connect();
				stmt = conn.prepareStatement("DELETE FROM Azioni WHERE ID = ?;");
				stmt.setInt(1, actionID);
				stmt.executeUpdate();
				break;
			} catch (SQLException e) {

			} finally {
				Database.closeConnection(conn, stmt);
			}
		} while (true);
	}

	private BigDecimal getCurrentActionValue(int resourceID) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String symbol = null;

		Buffer<BigDecimal> buf = new Buffer<BigDecimal>();
		try {
			conn = Database.connect();
			stmt = conn.prepareStatement("SELECT Symbol FROM Risorse WHERE ID = ?;");
			stmt.setInt(1, resourceID);
			rs = stmt.executeQuery();
			if(rs.next()) {
				symbol = rs.getString(1);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			Database.closeConnection(conn, stmt, rs);
		}

		if(symbol != null) {
			RequestParam[] params = new RequestParam[] {
				new RequestParam("symbol", symbol)
			};

			new JavaFetch(API_SERVER_PATH + "/api/getResourceValues", "GET", params).then((response) -> {
				ResourceValues values = (ResourceValues)response.bodyAsObject(ResourceValues.class);
				buf.write(values.getCurrentPrice());
			}).onException((e) -> {
				buf.write(null);
			});
		} else {
			buf.write(null);
		}

		return buf.read();
	}

	public BigDecimal getUserBalance(int userID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		BigDecimal toRet = null;
		try {
			conn = Database.connect();
			stmt = conn.prepareStatement("SELECT Conto FROM Utenti WHERE ID = ?;");
			stmt.setInt(1, userID);
			rs = stmt.executeQuery();
			if(rs.next()) {
				toRet = rs.getBigDecimal(1);
			}
		} catch (SQLException e) {

		} finally {
			Database.closeConnection(conn, stmt, rs);
		}

		return toRet;
	}

	private void registerAPI() {
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
							Database.closeConnection(conn, stmt, rs);
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
					Database.closeConnection(conn, stmt, rs);
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
							Database.closeConnection(conn, stmt, rs);
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
				Database.closeConnection(conn, stmt, rs);
			}
		});
		
		server.get("/api/getResourceValues", (req, res) -> {
			String symbol = req.getRequestParamValue("symbol");
			if(symbol != null) {
				RequestParam[] params = new RequestParam[] {
					new RequestParam("symbol", symbol),
					new RequestParam("token", API_KEY)
				};

				new JavaFetch("https://finnhub.io/api/v1/quote", "GET", params).then((response) -> {
					JsonObject jsonObject = null;
					try {
						jsonObject = JsonParser.parseString(response.bodyAsString()).getAsJsonObject();
					} catch (JsonParseException e) {
						res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
						return;
					}
					
					if(jsonObject.has("c") && jsonObject.has("d") && jsonObject.has("dp") && jsonObject.has("h") && jsonObject.has("l") && jsonObject.has("o") && jsonObject.has("pc")) {
						BigDecimal current, change, percentChange, hight, low, open, previousClose;
						try {
							current = jsonObject.get("c").isJsonNull() ? null : jsonObject.get("c").getAsBigDecimal();
							change = jsonObject.get("d").isJsonNull() ? null : jsonObject.get("d").getAsBigDecimal();
							percentChange = jsonObject.get("dp").isJsonNull() ? null : jsonObject.get("dp").getAsBigDecimal();
							hight = jsonObject.get("h").isJsonNull() ? null : jsonObject.get("h").getAsBigDecimal();
							low = jsonObject.get("l").isJsonNull() ? null : jsonObject.get("l").getAsBigDecimal();
							open = jsonObject.get("o").isJsonNull() ? null : jsonObject.get("o").getAsBigDecimal();
							previousClose = jsonObject.get("pc").isJsonNull() ? null : jsonObject.get("pc").getAsBigDecimal();
						} catch (NumberFormatException e) {
							res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
							return;
						}
						
						ResourceValues resourceValues = new ResourceValues(current, change, percentChange, hight, low, open, previousClose);
						res.send(resourceValues);
					} else {
						res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
					}
				}).onException((e) -> {
					res.status(500).send("Si è verificato un errrore durante l'ottenimento dei dati dall'API esterna");
				});

			} else {
				res.status(400).send("Parametro symbol assente");
			}
		});
		
		server.get("/api/getResourceValues", (req, res) -> {
			String symbol = req.getRequestParamValue("symbol");
			if(symbol != null) {
				RequestParam[] params = new RequestParam[] {
					new RequestParam("symbol", symbol),
					new RequestParam("token", API_KEY)
				};

				new JavaFetch("https://finnhub.io/api/v1/quote", "GET", params).then((response) -> {
					JsonObject jsonObject = null;
					try {
						jsonObject = JsonParser.parseString(response.bodyAsString()).getAsJsonObject();
					} catch (JsonParseException e) {
						res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
						return;
					}
					
					if(jsonObject.has("c") && jsonObject.has("d") && jsonObject.has("dp") && jsonObject.has("h") && jsonObject.has("l") && jsonObject.has("o") && jsonObject.has("pc")) {
						BigDecimal current, change, percentChange, hight, low, open, previousClose;
						try {
							current = jsonObject.get("c").isJsonNull() ? null : jsonObject.get("c").getAsBigDecimal();
							change = jsonObject.get("d").isJsonNull() ? null : jsonObject.get("d").getAsBigDecimal();
							percentChange = jsonObject.get("dp").isJsonNull() ? null : jsonObject.get("dp").getAsBigDecimal();
							hight = jsonObject.get("h").isJsonNull() ? null : jsonObject.get("h").getAsBigDecimal();
							low = jsonObject.get("l").isJsonNull() ? null : jsonObject.get("l").getAsBigDecimal();
							open = jsonObject.get("o").isJsonNull() ? null : jsonObject.get("o").getAsBigDecimal();
							previousClose = jsonObject.get("pc").isJsonNull() ? null : jsonObject.get("pc").getAsBigDecimal();
						} catch (NumberFormatException e) {
							res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
							return;
						}
						
						ResourceValues resourceValues = new ResourceValues(current, change, percentChange, hight, low, open, previousClose);
						res.send(resourceValues);
					} else {
						res.status(500).send("I dati ottenuti dall'API esterna sono invalidi");
					}
				}).onException((e) -> {
					res.status(500).send("Si è verificato un errrore durante l'ottenimento dei dati dall'API esterna");
				});

			} else {
				res.status(400).send("Parametro symbol assente");
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
							int ID = rs.getInt(1);
							String symbol =  rs.getString(2);
							String name =  rs.getString(3);
							exchanges.add(new Resource(ID, symbol, name));
						} while (rs.next());
						res.status(200).send(exchanges);
					} else {
						res.status(404).send("Non sono risorse per la borsa specificata nel database");
					}
				} catch (SQLException e) {
					res.status(500).send("Si è verificato un errore durante il recupero dei dati dal database");
				} finally {
					Database.closeConnection(conn, stmt, rs);
				}
			} else {
				res.status(400).send("Parametro MIC assente");
			}
		});
		
		server.post("/api/buyAction", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.disableExpiry();
			SessionVariable userIDVar = session.getSessionVariable("userID");
			if(userIDVar != null) {
				int userID = (Integer)userIDVar.getValue();
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
					BigDecimal valoreUnitario = null;
					try {
						valoreUnitario =  getCurrentActionValue(resourceID);
					} catch (SQLException e) {
						res.status(500).send("Errore interno al database");
						return;
					}

					if(valoreUnitario != null) {					
						BigDecimal daSottrarre = valoreUnitario.multiply(quantita); // valore da sottrarre al conto
						BigDecimal conto = getUserBalance(userID);
						System.out.println("TI SERVE : " + daSottrarre);
						System.out.println("CONTO: " + conto);
						if(conto != null && conto.subtract(daSottrarre).compareTo(BigDecimal.ZERO) >= 0) {
							Integer actionID = registerOrUpdateAction(userID, resourceID, quantita);
							if(actionID != null) {
								if(updateBalance(userID, daSottrarre)) {
									res.status(200).send("Azione acquistata correttamente");
								} else {
									deleteAction(actionID);
									res.status(500).send("Si è verificato un errore durante l'aggiornamento del conto");
								}
							} else {
								res.status(500).send("Si è verificato un errore durante il tentativo di comprare l'azione");
							}
						} else {
							res.status(400).send("Il conto non è sufficiente"); // TODO trovare un codice di errore opportuno
						}
					} else {
						res.status(400).send("La risorsa è inesistente!");
					}
				} else {
					res.status(400).send("Dati assenti");
				}
			} else {
				res.status(401).send("Non è stato ancora effettuato il login");
			}
		});


		server.post("/api/deposit", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.disableExpiry();
			String toDepositStr = req.getRequestParamValue("money");
			if(toDepositStr != null) {
				SessionVariable userIDStr = session.getSessionVariable("userID");
				if(userIDStr != null) {
					int userID = (Integer)userIDStr.getValue();
					BigDecimal toDeposit = null;

					try {
						toDeposit = new BigDecimal(toDepositStr);
					} catch (NumberFormatException e) {}

					if(toDeposit != null && toDeposit.scale() == 2 && toDeposit.compareTo(BigDecimal.ZERO) > 0) {
						if(updateBalance(userID, toDeposit)) {
							res.status(200).send("Conto aggiornato correttamente");
						} else {
							res.status(500).send("Si è verificato un errore durante l'aggiornamento del conto");
						}
					} else {
						res.status(400).send("Il parametro money non ha un valore valido");
					}
				} else {
					res.status(403).send("Devi effettuare il login");
				}
			} else {
				res.status(400).send("Parametro money assente");
			}
		});

		server.post("/api/withdraw", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.disableExpiry();
			String toWithdrawStr = req.getRequestParamValue("money");
			SessionVariable userIDVar = session.getSessionVariable("userID");
			if(userIDVar != null) {
				int userID = (Integer)userIDVar.getValue();
				if(toWithdrawStr != null) {
					BigDecimal toWithdraw = null;
					try {
						toWithdraw = new BigDecimal(toWithdrawStr);
					} catch (NumberFormatException e) {}
					if(toWithdraw != null && toWithdraw.scale() == 2 && toWithdraw.compareTo(BigDecimal.ZERO) > 0) {
						BigDecimal balance = getUserBalance(userID);
						if(balance != null) {
							if(balance.compareTo(toWithdraw) >= 0) {
								if(updateBalance(userID, toWithdraw.multiply(new BigDecimal(-1)))) {
									res.status(200).send("Prelievo effettuato correttamente");
								} else {
									res.status(500).send("Si è verificato un errore durante il prelievo dal conto");
								}
							} else {
								res.status(400).send("Non sono presenti abbastanza soldi nel conto per completare l'operazione");
							}
						} else {
							res.status(500).send("Si è verificato un errore interno al database");
						}
					} else {
						res.status(400).send("Il parametro money non ha un valore valido");
					}
				} else {
					res.status(400).send("Parametro money assente");
				}
			} else {
				res.status(403).send("Devi effettuare il login");
			}
		});
	}
}
