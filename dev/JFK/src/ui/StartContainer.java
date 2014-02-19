package ui;

import javax.servlet.http.HttpServletRequest;

import resources.ResourcePaths;
import util.HTMLUtil;
import model.User;

public class StartContainer extends AbstractContainer {

	@Override
	public void provideContent(HttpServletRequest request,
			StringBuilder content, User currentUser) {
		content.append("Welcome to the StartContainer page! This will be the overview. <br \\>");
		content.append(HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/startPage.html", null));
		
	}
}
