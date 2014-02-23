package ui;

import javax.servlet.http.HttpServletRequest;

import resources.ResourcePaths;
import util.HTMLUtil;
import model.User;

public class LoginContainer extends AbstractContainer{

	@Override
	public void provideContent(HttpServletRequest request,
			StringBuilder content, User currentUser) {
		// TODO Auto-generated method stub
		content.append(HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/login.html", null));
	}

}
