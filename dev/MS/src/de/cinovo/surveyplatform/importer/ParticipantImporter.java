package de.cinovo.surveyplatform.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.exporter.ExportContainer;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.sync.SyncCloneCallback;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncPreSyncCallback;
import de.cinovo.surveyplatform.sync.Synchronizer;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ParticipantImporter {
	
	/**
	 *
	 */
	private static final String SET_PROPERTY_METHOD_PREFIX = "setProperty:";
	private static final int SET_PROPERTY_METHOD_PREFIX_LENGTH = ParticipantImporter.SET_PROPERTY_METHOD_PREFIX.length();
	
	private final Map<String, String> columns = new HashMap<String, String>();
	private final String feedBackTaskID;
	
	
	/**
	 *
	 */
	public ParticipantImporter(final String feedBackTaskID) {
		this.feedBackTaskID = feedBackTaskID;
		this.prepareExcelColumns();
	}
	
	/**
	 *
	 */
	private void prepareExcelColumns() {
		this.columns.put("name", "setSurname");
		this.columns.put("surname", "setSurname");
		this.columns.put("surnameparticipant", "setSurname");
		this.columns.put("firstname", "setName");
		this.columns.put("firstnameparticipant", "setName");
		this.columns.put("programme/coursetitle", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "programmeTitle");
		this.columns.put("programme", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "programmeTitle");
		this.columns.put("coursetitle", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "programmeTitle");
		this.columns.put("nqflevel", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "nqfLevel");
		this.columns.put("nqf", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "nqfLevel");
		this.columns.put("18.1/18.2", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "a18_1_18_2");
		this.columns.put("contactphone", "setContactPhone");
		this.columns.put("contactemail", "setContactEmail");
		this.columns.put("homelanguage", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "homeLanguage");
		this.columns.put("race", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "ethnicgroup");
		this.columns.put("ethnicgroup", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "ethnicgroup");
		this.columns.put("gender", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "gender");
		this.columns.put("age", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "age");
		this.columns.put("province", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "province");
		this.columns.put("employername", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "a01employerName");
		this.columns.put("phoneemployer", ParticipantImporter.SET_PROPERTY_METHOD_PREFIX + "a02employerPhone");
	}
	
	/**
	 * Imports Participants from an Excel file (xls or xlsx). The format of the
	 * file must be as follows:<br>
	 * <br>
	 * <ul>
	 * <li>In the first line of the table, there must be the column names.</li>
	 * <li>This importer looks for "email", "name", "phone". At least these
	 * three columns must be present.</li>
	 * <li>Order is not relevant.</li>
	 * <li>Other columns may placed between these three recognized by the
	 * importer.</li>
	 * <li>The Importer imports all records that have at least "email"
	 * specified.</li>
	 * </ul>
	 * Example: Importing the following table<br>
	 * <table>
	 * <tr>
	 * <td>id</td>
	 * <td>name</td>
	 * <td>address</td>
	 * <td>email</td>
	 * <td>phone</td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td>A</td>
	 * <td>B</td>
	 * <td>C</td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>E</td>
	 * <td>F</td>
	 * <td>G</td>
	 * <td>H</td>
	 * </tr>
	 * </table>
	 * will result in two Participants with the names {A, E}, the emails {C, G}
	 * and the phonenumbers {'', H}. I.e. the columns id and address are ignored
	 * 
	 * 
	 * @param file Excel file to be imported
	 * @return List of Participants. Returns an empty list, if no Participant
	 *         could be imported
	 * @throws ImportFromExcelException
	 */
	public List<Participant> importFromExcel(final File file, final SystemUser currentUser) throws ImportFromExcelException {
		List<Participant> participants = new ArrayList<Participant>();
		Workbook workbook = null;
		
		if (!file.exists()) {
			throw new ImportFromExcelException(file, ImportFromExcelException.REASON_NOT_EXISTING);
		}
		
		FileInputStream fileInputStream = null;
		try {
			
			fileInputStream = new FileInputStream(file);
			
			if (file.getName().endsWith(".xlsx")) {
				workbook = new XSSFWorkbook(fileInputStream);
			}
			if (file.getName().endsWith(".xls")) {
				workbook = new HSSFWorkbook(fileInputStream);
			}
		} catch (Exception e) {
			try {
				fileInputStream.close();
			} catch (IOException e1) {
				// ignore
			}
			throw new ImportFromExcelException(e);
		}
		
		if (workbook != null) {
			Sheet sheet = workbook.getSheetAt(0);
			int rows = sheet.getPhysicalNumberOfRows();
			List<Integer> failureRows = new ArrayList<Integer>();
			int successRows = 0;
			if (rows == 0) {
				throw new ImportFromExcelException(file, ImportFromExcelException.REASON_INVALID_FORMAT);
			} else {
				
				Row row = null;
				int headRow = 0;
				Map<Integer, String> usedColumns = new HashMap<Integer, String>();
				for (headRow = 0; (headRow < 20) && (headRow < rows); headRow++) {
					row = sheet.getRow(headRow);
					if (row != null) {
						
						// guess columns
						Map<String, Integer> columnMap = this.readColumnNumbers(row);
						usedColumns.clear();
						for (Entry<String, String> entry : this.columns.entrySet()) {
							Integer columnIndex = columnMap.get(entry.getKey());
							if (columnIndex != null) {
								usedColumns.put(columnIndex, entry.getValue());
							}
						}
						if (usedColumns.size() > 0) {
							break;
						}
					}
				}
				
				if (row != null) {
					for (int r = headRow + 1; r < rows; r++) {
						row = sheet.getRow(r);
						if (row == null) {
							continue;
						}
						try {
							Participant p = SurveyElementFactory.getInstance().createParticipant(null, false);
							int propertyCount = 0;
							for (Entry<Integer, String> entry : usedColumns.entrySet()) {
								final String methodName = entry.getValue();
								
								final Cell cell = row.getCell(entry.getKey());
								if (cell != null) {
									final String cellValue = cell.toString();
									if (!cellValue.isEmpty()) {
										if (methodName.startsWith(ParticipantImporter.SET_PROPERTY_METHOD_PREFIX)) {
											p.getProperties().setProperty(methodName.substring(ParticipantImporter.SET_PROPERTY_METHOD_PREFIX_LENGTH), cellValue);
										} else {
											Participant.class.getMethod(methodName, String.class).invoke(p, cellValue);
										}
										propertyCount++;
									}
								}
							}
							// only add participant if any of the rows could be
							// read
							if (propertyCount == 0) {
								failureRows.add((r + 1));
							} else {
								participants.add(p);
								successRows++;
							}
						} catch (Exception e) {
							failureRows.add((r + 1));
							Logger.err("Could not read row " + r + " from " + file.getAbsolutePath(), e);
						}
					}
				}
				FeedBackProvider fbp = FeedBackProvider.getInstance();
				if (successRows == 0) {
					throw new ImportFromExcelException(file, ImportFromExcelException.REASON_NO_PARTICIPANTS_FOUND);
				} else if (failureRows.size() > 0) {
					StringBuilder failureRowsConcat = new StringBuilder();
					int count = 0;
					for (Integer rowIndex : failureRows) {
						count++;
						if (count > 20) {
							failureRowsConcat.append(" and " + (failureRows.size() - 20) + " more");
							break;
						}
						failureRowsConcat.append(", ");
						failureRowsConcat.append(rowIndex);
					}
					fbp.addFeedback(this.feedBackTaskID, "Could not read the lines " + failureRowsConcat.substring(1), Status.WARNING, currentUser.getActualUserName());
				}
			}
			try {
				fileInputStream.close();
			} catch (IOException e1) {
				// ignore
			}
		} else {
			try {
				fileInputStream.close();
			} catch (IOException e1) {
				// ignore
			}
			throw new ImportFromExcelException(file, ImportFromExcelException.REASON_NOT_VALID_EXCEL);
		}
		return participants;
	}
	
	private Map<String, Integer> readColumnNumbers(final Row row) {
		int cells = row.getPhysicalNumberOfCells();
		
		Map<String, Integer> columnMap = new HashMap<String, Integer>();
		for (int i = 0; i < cells; i++) {
			Cell cell = row.getCell(i);
			if (cell != null) {
				columnMap.put(cell.getStringCellValue().toLowerCase().replace(" ", "").replace(":", "").trim(), i);
			}
		}
		return columnMap;
	}
	
	@SuppressWarnings("unchecked")
	public void importFromXml(final File file, final SystemUser currentUser) throws ImportFromXmlException {
		final XStream xstream = new XStream(new StaxDriver());
		
		ExportContainer exportContainer = (ExportContainer) xstream.fromXML(file);
		List<Participant> participants = null;
		
		// search participant list in the export container
		for (Object o : exportContainer.getContent()) {
			if (o instanceof List<?>) {
				for (Object p : (List<?>) o) {
					if (p instanceof Participant) {
						participants = (List<Participant>) o;
						break;
					}
				}
			}
			if (participants != null) {
				break;
			}
		}
		
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		if (participants == null) {
			fbp.addFeedback(this.feedBackTaskID, "No participant information found in the xml file: " + file.getName(), Status.ERROR, currentUser.getActualUserName());
			return;
		}
		final Session session = HibernateUtil.getSessionFactory().openSession();
		int importedCount = 0;
		int totalParticipants = participants.size();
		try {
			Transaction tx = session.beginTransaction();
			for (Participant importedParticipant : participants) {
				Participant storedParticipant = null;
				{
					Criteria criteria = session.createCriteria(Participant.class);
					criteria.add(Restrictions.eq("syncId", importedParticipant.getSyncId()));
					storedParticipant = (Participant) criteria.uniqueResult();
				}
				Survey survey = null;
				if (storedParticipant == null) {
					if (EnvironmentConfiguration.isOfflineMode()) {
						Participant newParticipant = importedParticipant.cloneWithId();
						if (survey == null) {
							Criteria criteria = session.createCriteria(Survey.class);
							String syncId = importedParticipant.getSurvey().getSyncId();
							criteria.add(Restrictions.eq("syncId", syncId));
							survey = (Survey) criteria.uniqueResult();
							if (survey == null) {
								survey = new Survey();
								survey.setSyncId(syncId);
								Synchronizer sync = new Synchronizer();
								sync.leftToRight(importedParticipant.getSurvey(), survey, SyncFilter.SURVEY);
								survey.setParticipants(new ArrayList<Participant>());
								survey.setId(importedParticipant.getSurvey().getId());
								session.save(survey);
							}
						}
						
						newParticipant.setSurvey(survey);
						if (importedParticipant.getParticipation() != null) {
							newParticipant.getParticipation().setId(importedParticipant.getParticipation().getId());
						}
						survey.getParticipants().add(newParticipant);
						session.save(newParticipant);
						fbp.addFeedback(this.feedBackTaskID, "Created a new participant: " + importedParticipant.getName() + importedParticipant.getSurname(), Status.OK, currentUser.getUserName());
					} else {
						fbp.addFeedback(this.feedBackTaskID, "Skipped participant: " + importedParticipant.getName() + importedParticipant.getSurname() + " (does not exist in the database)", Status.WARNING, currentUser.getActualUserName());
					}
				} else {
					Synchronizer sync = new Synchronizer();
					sync.setCloneCallback(new SyncCloneCallback() {
						
						@Override
						public Object clone(final Object toClone) {
							if (toClone instanceof Participation) {
								((Participation) toClone).cloneWithId();
							}
							if (toClone instanceof Questionnaire) {
								return ((Questionnaire) toClone).cloneWithId();
							}
							
							return null;
						}
					});
					sync.setPreSyncCallback(new SyncPreSyncCallback() {
						
						@Override
						public void preSync(final Object left, final Object leftFieldValue, final Object right, final Object rightFieldValue, final String fieldName) {
							if (right instanceof Participation) {
								session.merge(right);
							}
						}
					});
					
					if (importedParticipant.getSurveySubmitted() != null) {
						sync.leftToRight(importedParticipant, storedParticipant, SyncFilter.PARTICIPANT);
						session.update(storedParticipant);
						importedCount++;
					}
				}
			}
			tx.commit();
			
			fbp.addFeedback(this.feedBackTaskID, "Imported the data of " + importedCount + " participants (total: " + totalParticipants + ")", Status.OK, currentUser.getActualUserName());
			
		} catch (Exception ex) {
			Logger.err("Error during Participant import", ex);
			fbp.addFeedback(this.feedBackTaskID, "There was an error during import of a participant: " + ex.getMessage(), Status.ERROR, currentUser.getActualUserName());
			throw new ImportFromXmlException(file, ImportFromXmlException.REASON_INVALID_FORMAT);
		} finally {
			session.close();
		}
		
	}
}
