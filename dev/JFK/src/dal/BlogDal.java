package dal;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;

import model.BlogEntry;
import model.BlogType;
import servlets.AbstractSccServlet;
import util.AuthorizationUtil;
import util.HibernateUtil;
import util.ParamUtil;

public class BlogDal extends AbstractSccServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 12321L;
	
	private static final String PARAM_BLOG_TITLE = "blogTitle";
	
	private static final String PARAM_BLOG_TEXT = "blogText";
	
	private static final String PARAM_BLOG_TYPE = "blogType";

	private static final int CONSTANT_INTRO_LENGTH = 150;
	
	public void processCreate(final HttpServletRequest req,
			final HttpServletResponse resp) throws IOException {
		if (AuthorizationUtil.checkAuthorization(req) != null){
			Session hibSess = null;
			try{
				hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				String blogTitle = ParamUtil.getSafeParam(req, PARAM_BLOG_TITLE);
				String blogText = ParamUtil.getSafeParam(req, PARAM_BLOG_TEXT);
				String blogType = ParamUtil.getSafeParam(req, PARAM_BLOG_TYPE);
				BlogEntry blogEntry = new BlogEntry();
				if (!(blogTitle.equals("") || blogText.equals("") || blogType.equals(""))){
					blogEntry.setHeadLine(blogTitle);
					blogEntry.setText(blogText);
					blogEntry.setBlogType(getBlogTypeFromString(blogType));
					blogEntry.setLastModified(new Date());
					blogEntry.setPreviewLength(CONSTANT_INTRO_LENGTH);
					hibSess.save(blogEntry);
					tx.commit();
				}
			} catch (Exception e){
				System.out.println(e);
			} finally {
				if (hibSess != null){
					hibSess.close();
				}
			}
			resp.sendRedirect(getStandardRedirectLocation(req));
		}
	}

	private BlogType getBlogTypeFromString(String blogTypeString) {
		if(blogTypeString.equalsIgnoreCase(BlogType.QUOTE_MONDAY.toString())){
			return BlogType.QUOTE_MONDAY;
		} else if (blogTypeString.equalsIgnoreCase(BlogType.STORYTELLING.toString())){
			return BlogType.STORYTELLING;
		} else if (blogTypeString.equalsIgnoreCase(BlogType.ESSAYS.toString())){
			return BlogType.ESSAYS;
		} else if (blogTypeString.equalsIgnoreCase(BlogType.JOURNALISM.toString())){
			return BlogType.JOURNALISM;
		} else if (blogTypeString.equalsIgnoreCase(BlogType.PR_AND_CONTENTMARKETING.toString())){
			return BlogType.PR_AND_CONTENTMARKETING;
		} else {
			return BlogType.OTHER;
		}
	}
}
