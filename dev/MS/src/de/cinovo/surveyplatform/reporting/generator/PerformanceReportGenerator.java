/**
 *
 */
package de.cinovo.surveyplatform.reporting.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jfree.chart.plot.PlotOrientation;

import de.cinovo.surveyplatform.chart.SccChart;
import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.chart.BarChartInfo;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.reporting.AccessReportFromDB;
import de.cinovo.surveyplatform.model.reporting.GenericReportInfo;
import de.cinovo.surveyplatform.model.reporting.IReport;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil.QuestionType;


/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class PerformanceReportGenerator extends AbstractReportGenerator {
	
	/**
	 * @param baseReport
	 * @param survey
	 */
	public PerformanceReportGenerator(final IReport baseReport, final Survey survey) {
		super(baseReport, survey);
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.reporting.generator.AbstractReportGenerator#
	 * generatePDFs(java.lang
	 * .StringBuilder, java.lang.StringBuilder, java.lang.String)
	 */
	@Override
	protected void generatePDF(final StringBuilder content, final SubTypeEnum subType) {
		generatePDFReport(content, getBaseTargetPath(), "_quantitative");
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.reporting.generator.AbstractReportGenerator#getContent(de.cinovo.surveyplatform.model.question.AbstractQuestion
	 * , org.hibernate.Session, de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia, java.lang.String)
	 */
	@Override
	protected StringBuilder getContent(final AbstractQuestion question, final Session hibSess, final TargetMedia targetMedia, final String taskID) {
		return new StringBuilder("This type of report is not supported by this report!");
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @seescc.reporting.generator.AbstractReportGenerator#getContent(de.cinovo.
	 * surveyplatform.util.
	 * QuestionnaireViewUtil.QuestionType, org.hibernate.Session,
	 * de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia)
	 */
	@Override
	protected StringBuilder getContent(final QuestionType type, final Session hibSess, final TargetMedia targetMedia, final String taskID) {
		StringBuilder questionnaireContent = new StringBuilder();
		questionnaireContent.append("<div class=\"questionarePage\">");
		questionnaireContent.append("<div class=\"section\"><p class=\"sectionTitle\">Performance Index of " + survey.getName() + "</p>");
		
		Criteria criteria = hibSess.createCriteria(AbstractQuestion.class);
		GenericReportInfo genRep = AccessReportFromDB.getInstance().getReportInfoById(Integer.parseInt(baseReport.getId()), hibSess);
		List<Integer> idList = genRep.getQuestions();
		criteria.add(Restrictions.in("id", idList));
		
		List<?> questionList = criteria.list();
		
		Map<Integer, List<String>> questionOptionMap = new HashMap<Integer, List<String>>();
		// this is to get all possible values and their order, to be able to
		// calculate a value
		for (Page p : survey.getQuestionnaire().getPages()) {
			for (Section s : p.getSections()) {
				for (AbstractQuestion q : s.getQuestions()) {
					for (Object o : questionList) {
						if (o instanceof AbstractQuestion) {
							AbstractQuestion qRef = (AbstractQuestion)o;
							if (q.getLocalId() == qRef.getLocalId()) {
								List<String> options = new ArrayList<String>();
								List<Option> originOptions = q.getOptions();
								for (int i = originOptions.size() - 1; i >= 0; i--) {
									options.add(originOptions.get(i).getDisplayName());
								}
								questionOptionMap.put(qRef.getLocalId(), options);
							}
						}
					}
					
				}
			}
			
		}
		
		Map<String, Map<Integer, Integer>> valueMap = new HashMap<String, Map<Integer, Integer>>();
		
		for (Object o : questionList) {
			if (o instanceof AbstractQuestion) {
				AbstractQuestion qRef = (AbstractQuestion) o;
				
				StringBuilder queryStr = new StringBuilder("Select o from " + qRef.getClass().getCanonicalName() + " q JOIN q.options as o ");
				queryStr.append("WHERE o.originQuestionId = q.originQuestionId AND q.localId = " + qRef.getLocalId() + " AND o.submitted = TRUE AND o.selected = TRUE");
				try {
					List<?> list = hibSess.createQuery(queryStr.toString()).list();
					
					Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
					
					for (Object ob : list) {
						if (ob instanceof Option) {
							List<String> values = questionOptionMap.get(qRef.getLocalId());
							
							// wertigkeit der antwort berechnen
							int value = values.indexOf(((Option) ob).getDisplayName()) + 1;
							if (!countMap.containsKey(value)) {
								countMap.put(value, 0);
							}
							countMap.put(value, countMap.get(value) + 1);
							
						}
					}
					
					valueMap.put(qRef.getQuestion(), countMap);
				} catch (Exception e) {
					Logger.err("Error creating Report:\n Survey: " + survey.getId() + "\n Report: " + baseReport.getId() + "\n File: " + Paths.REPORTS + "/" + survey.getId() + "/" + baseReport.getId() + ".xml");
					questionnaireContent.append("Sorry, this Report could not be generated. Please contact your support!");
				}
			}
		}
		
		Map<String, Double> dataSet = new LinkedHashMap<String, Double>();
		
		BarChartInfo barChartInfo = new BarChartInfo();
		barChartInfo.setTitle("");
		barChartInfo.setxAxisLabel("");
		barChartInfo.setyAxisLabel("");
		barChartInfo.setOrientation(PlotOrientation.VERTICAL);
		
		for (Entry<String, Map<Integer, Integer>> entry : valueMap.entrySet()) {
			double average = 0;
			int count = 0;
			for (Entry<Integer, Integer> countMapEntry : entry.getValue().entrySet()) {
				average += countMapEntry.getKey() * countMapEntry.getValue();
				count += countMapEntry.getValue();
			}
			average /= count;
			dataSet.put(entry.getKey(), average);
		}
		
		barChartInfo.addDataSet(new DataSetContainer("", dataSet));
		
		barChartInfo.setNumberOfResponses(Arrays.asList(new Integer[] {0}));
		barChartInfo.setHeight(250);
		int width = 80 + (int) Math.round(80 * dataSet.size() * 0.90);
		barChartInfo.setWidth(width > SccChart.MAX_WIDTH ? SccChart.MAX_WIDTH : width);
		
		String filePath = survey.getId() + "/" + getIdentifier() + "/chart";
		if (SccChart.OUTPUTTYPE.equals("PNG")) {
			questionnaireContent.append("<div style=\"text-align: center; margin: 10px;\"><img title=\"Save or Copy this graph into any Document: Right-Click / Copy... Paste\" src=\"" + (new SccChart().createChart(filePath, ChartType.bar, barChartInfo)) + "\" /></div>");
		}
		
		questionnaireContent.append("</div>");
		questionnaireContent.append("</div>");
		// }
		return questionnaireContent;
	}
	
}
