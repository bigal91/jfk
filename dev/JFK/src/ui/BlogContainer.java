package ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import constants.Paths;
import util.AuthorizationUtil;
import util.HTMLUtil;
import util.HibernateUtil;
import util.ParamUtil;
import model.BlogEntry;
import model.BlogType;
import model.User;

public class BlogContainer  extends AbstractContainer {

	private static final String PARAM_COLUMN = "column";

	@Override
	public void provideContent(HttpServletRequest request,
			StringBuilder content, User currentUser) {
		Map<String,String> replacements = new HashMap<String, String>();
		if (currentUser != null){
			// give "ADD BLOGENTRY" Option
			replacements.put("ADD_BLOGENTRY", HTMLUtil.getHTMLFile(Paths.HTML_PATH + "/button_addBlogEntry.html", null));
			
		}
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		Criteria crit = hibSess.createCriteria(BlogEntry.class);
		if(ParamUtil.checkAllParamsSet(request, PARAM_COLUMN)){
			// set a specific column as "marked"
			String paramColumn = ParamUtil.getSafeParam(request, PARAM_COLUMN);
			BlogType type = identifyBlogType(paramColumn);
			crit.add(Restrictions.eq("blogType", type));
		}
		// TODO iterate through all BlogEntries and set them in replacements
		StringBuilder blogContent = new StringBuilder();
		List<?> blogList = crit.list();
		for(Object obj : blogList){
			BlogEntry blogEntry = (BlogEntry) obj;
			replacements.put("BLOG_TITLE", blogEntry.getHeadLine());
			replacements.put("BLOG_TEXT", getBlogEntryPreview(blogEntry));
			blogContent.append(HTMLUtil.getHTMLFile(Paths.HTML_PATH + "/blogEntry.html", replacements));
		}
		replacements.put("CONTENT", blogContent.toString());
		replacements.put("BLOG_CONTENT", HTMLUtil.getHTMLFile(Paths.HTML_PATH + "/blogContent.html", replacements));
		content.append(HTMLUtil.getHTMLFile(Paths.HTML_PATH + "/blog.html", replacements));
	}

	/* 				HELPER METHODS 				*/
	
	private String getBlogEntryPreview(BlogEntry entry) {
		// TODO clear escaped HTML artifacts in last few chars of string
		if (entry.getText().length() > entry.getPreviewLength()){
			return entry.getText().substring(0, entry.getPreviewLength()) + "...";
		}
		return entry.getText().substring(0, entry.getPreviewLength());
	}

	private BlogType identifyBlogType(String paramColumn) {
		if (paramColumn.equalsIgnoreCase(BlogType.QUOTE_MONDAY.toString())){
			return BlogType.QUOTE_MONDAY;
		} else if(paramColumn.equalsIgnoreCase(BlogType.STORYTELLING.toString())){
			return BlogType.STORYTELLING;
		}else if(paramColumn.equalsIgnoreCase(BlogType.ESSAYS.toString())){
			return BlogType.ESSAYS;
		}else if(paramColumn.equalsIgnoreCase(BlogType.JOURNALISM.toString())){
			return BlogType.JOURNALISM;
		}else if(paramColumn.equalsIgnoreCase(BlogType.PR_AND_CONTENTMARKETING.toString())){
			return BlogType.PR_AND_CONTENTMARKETING;
		}else if(paramColumn.equalsIgnoreCase(BlogType.OTHER.toString())){
			return BlogType.OTHER;
		}
		return null;
	}

}
