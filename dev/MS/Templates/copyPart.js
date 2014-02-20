if (move{PART}ID == -1) {
	if (copy{PART}ID == '{ID}') {
		copy{PART}ID = -1;
		$('.insert{PART}').fadeOut(200, function(){$('.insert{PART}PlaceHolder').show();});
	} else if(copy{PART}ID == -1) {
		copy{PART}ID = '{ID}';
		$('.insert{PART}PlaceHolder').hide();
		$('.insert{PART}').fadeIn(200);
	} else {
		$('#cpi' + copy{PART}ID).toggleClass('gui-icon-button-PAGE_COPY_ENABLED');
		copy{PART}ID = '{ID}';
	}
	$(this).toggleClass('gui-icon-button-PAGE_COPY_ENABLED');
}