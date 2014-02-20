	var flobu ;
	var loadComplete = true;
	var cssImports = 	'<link href="css_ext/reset.css" media="screen" type="text/css" rel="stylesheet">' +
						'<link href="css_ext/redmond/jquery-ui.css" media="screen" type="text/css" rel="stylesheet">' +
						'<link href="css_ext/scc.css" media="screen" type="text/css" rel="stylesheet">' +
						'<link href="css_ext/datatable/datatable.css" media="screen" type="text/css" rel="stylesheet">' +
						'<link href="css_ext/datatable/datatable_page.css" media="screen" type="text/css" rel="stylesheet">';


	jQuery.fn.stripTags = function() { return this.html( this.html().replace(/<br(\s)*(\/)?>/gi, '__LINEBREAK__').replace(/<\/?[^>]+>/gi, '').replace(/__LINEBREAK__/g, '<br />') ); };
	
	jQuery.fn.sortElements = (function(){
		 
	    var sort = [].sort;
	 
	    return function(comparator, getSortable) {
	 
	        getSortable = getSortable || function(){return this;};
	 
	        var placements = this.map(function(){
	 
	            var sortElement = getSortable.call(this),
	                parentNode = sortElement.parentNode,
	 
	                // Since the element itself will change position, we have
	                // to have some way of storing its original position in
	                // the DOM. The easiest way is to have a 'flag' node:
	                nextSibling = parentNode.insertBefore(
	                    document.createTextNode(''),
	                    sortElement.nextSibling
	                );
	 
	            return function() {
	 
	                if (parentNode === this) {
	                    throw new Error(
	                        "You can't sort elements if any one is a descendant of another."
	                    );
	                }
	 
	                // Insert before flag:
	                parentNode.insertBefore(this, nextSibling);
	                // Remove flag:
	                parentNode.removeChild(nextSibling);
	 
	            };
	 
	        });
	 
	        return sort.call(this, comparator).each(function(i){
	            placements[i].call(getSortable.call(this));
	        });
	 
	    };
	 
	})();	

	function showWait(taskID) {

		if (typeof taskID != 'undefined') {
			// pollTaskInfo(taskID);
		}

		loadComplete = false;
		setTimeout(function() {
			if (!loadComplete) {
				flobu.enable();
			}
		}, 1000);

		return true;
	}
	function hideWait() {
		loadComplete = true;
		flobu.disable();
	}

	var _cacheCount = 0;
	function ajax(options) {
		if (typeof options.showWait == 'undefined' || options.showWait) {
			showWait();
		}
		if (options.url.indexOf('?') == -1) {
			options.url += "?nocache=" + (_cacheCount++).toString();
		} else {
			options.url += "&nocache=" + (_cacheCount++).toString();
		}
		$.ajax({
			url: options.url,
			type: options.type,
			data: options.data,
			success: options.success,
			error: options.error,
			complete: function (xhr, status) {
				if (typeof options.showWait == 'undefined' || options.showWait) {
					hideWait();
				}
				if (xhr.status != 200 && xhr.status > 0) {
				    var msg = "Sorry but there was an unexpected error.\nPlease contact the system administrators.\nMessage: ";
				    alert(msg + xhr.status + " " + xhr.statusText);
				}
			}
		});
	}

	function maximizeDialog(id) {
		$("#" + id).dialog( "option", "height", $(window).height() * 0.95);
		$("#" + id).dialog( "option", "width", $(window).width() * 0.95);
		$("#" + id).dialog( "option", "position", "center");
	}

	function augmentDialogWithMaximizeButton(dialog, dialogId) {
		var titlebar = dialog.parent().find('.ui-dialog-titlebar');
		if (titlebar.has("a.ui-dialog-titlebar-maximize").length == 0) {
			titlebar.append('<a class="ui-dialog-titlebar-maximize ui-corner-all" onclick="maximizeDialog(\'' + dialogId + '\')"  role=button href="#"><SPAN class="ui-icon ui-icon-newwin">Maximize</SPAN></a>');
		}
	}

	function getFeedBack(taskID, targetElement) {
		setTimeout(function() {
			ajax({
				url: (_baseurl + "/feedback/retrieve?html=1&taskId=" + taskID),
				type: "GET",
				success:
					function (result) {
						if (result != "") {
							$(targetElement).html(result);
						}
					}
			});
		}, 500);
	}

	var feedBackPoller;
	var feedBackPollerRunning = false;
	function pollTaskInfo(taskID) {

		if (!feedBackPollerRunning) {
			feedBackPollerRunning = true;
			feedBackPoller = setInterval(function() {
				ajax({
					showWait: false,
					url: (_baseurl + "/feedback/retrieve?json=1&taskId=" + taskID),
					type: "GET",
					success:
						function (result) {
							if (result != "") {
								var obj = jQuery.parseJSON(result);
								if (obj != null) {
									if (obj.statusCode == 'FINISHED') {
										clearInterval(feedBackPoller);
										feedBackPollerRunning = false;
										$('#taskName').text("");
										$('#taskProgress').text("");
									} else {
										$('#taskName').text(obj.taskName);
										$('#taskProgress').text(obj.progress + "%");
									}
								}
							}
						},
						error: function () {
							alert("wrong");
						}
				});
			}, 500);
		}
	}


	function openHelp(helpId) {
		
		var dynamicWidth = 500;
		var dynamicHeight = 400;
		if (helpId == 'dlg.chainLogic'){
			dynamicWidth = 900;
			dynamicHeight = 550;
		}
		$("#contextHelpDialog").dialog({
			autoOpen: false,
			height: dynamicHeight,
			width: dynamicWidth,
			modal: true,
			maximize: true,
			open: function(event, ui) {
				ajax({
					url: (_baseurl + "/help/retrieve?helpId=" + helpId),
					type: "GET",
					success:
						function (result) {
							if (result != "") {
								$('#contextHelpDialog').html(result);
							}
						}
				});
				// augmentDialogWithMaximizeButton($(this), 'contextHelpDialog');
			},
			buttons: {
				'Close': function() {
					$(this).dialog('close');
					$(this).dialog('destroy');
				}
			},
			Cancel: function() {
				$(this).dialog('close');
				$(this).dialog('destroy');
			}
		});

		$('#contextHelpDialog').dialog('open');
	}

	function augmentDataTableHeadersWithTooltips() {
		$('.dataTables_wrapper table th.sorting').each(function(index) {
			$(this).attr('title', 'Click to Sort by ' + $(this).text() + '. The arrow on the right indicates sorting ascending or descending.');
		});
		$('.dataTables_wrapper table th.sorting_asc').each(function(index) {
			$(this).attr('title', 'Click to Sort by ' + $(this).text() + '. The arrow on the right indicates sorting ascending or descending.');
		});
		$('.dataTables_wrapper table th.sorting_desc').each(function(index) {
			$(this).attr('title', 'Click to Sort by ' + $(this).text() + '. The arrow on the right of each column indicates sorting ascending or descending.');
		});
	}

	function downloadHtmlFragment(titleOfQuestion, htmlObject){
		var newHtmlFile = "<html>";
		newHtmlFile += cssImports;
		newHtmlFile += "<body>";
		newHtmlFile += "<center><b>" + titleOfQuestion + "</b></center><br /><br /><br />";
		newHtmlFile += "<p>"+ htmlObject +"</p>";
		newHtmlFile += "</body></html>";
		var j = window.open('');
		j.document.write(newHtmlFile);
		j.document.close();
	}

	$(document).ready(function(){
		$(function(){
			flobu = new flower_bubble ({
				base_obj: $('body'),
				base_dir: document.location.pathname,
				background: { css: 'white', opacity: 0.58 },
				bubble: { image: '../gfx/bubble.png', width: 130, height: 98 },
				flower: { image: '../gfx/loader.gif', width: 32, height: 32 },
				block_mode: 'full'
			}) ;
		});

		// augment the datatables with tooltips for sorting

/*		var oTable = $('div.dataTables_scrollBody>table.display').dataTable();
		if ( oTable.length > 0 ) {
			oTable.fnAdjustColumnSizing();
		}*/
		
		$('.ui-dialog-titlebar-maximize').live('mouseenter', function() {
			$(this).addClass('ui-state-hover');
		});
		$('.ui-dialog-titlebar-maximize').live('mouseleave', function() {
			$(this).removeClass('ui-state-hover');
		});


	});


	function getSelectionInfo(element){
	    var t = {"text" : '', "start": -1, "end": -1};
	    if (window.getSelection){
	    	var selection = window.getSelection().toString();
	        t.text = selection;
	    } else if (document.getSelection){
	        t.text = document.getSelection().toString();
	    } else if (document.selection){
	        t.text = document.selection.createRange().text;
	    }
	    
	    t.text = $.trim(t.text); 
	    // this tries to find the index of the selection
	    // => not accurate when selecting something that
	    // occurs multiple times in the elements text
	    t.start = element.text().indexOf(t.text);
	    t.end = t.start >= 0? (t.start + t.text.length):-1; 

	    return t;
	}



