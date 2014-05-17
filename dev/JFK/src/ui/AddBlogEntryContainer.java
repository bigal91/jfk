package ui;

import javax.servlet.http.HttpServletRequest;

import constants.Paths;
import util.HTMLUtil;
import model.User;

public class AddBlogEntryContainer  extends AbstractContainer {

	@Override
	public void provideContent(HttpServletRequest request,
			StringBuilder content, User currentUser) {
		if (currentUser != null){
			content.append(HTMLUtil.getHTMLFile(Paths.HTML_PATH + "/editBlogEntry.html", null));			
		} else {
			content.append("Error - Insufficient Permission.");
		}
		
	}

}
