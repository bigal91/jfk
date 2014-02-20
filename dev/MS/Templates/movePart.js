if (copy{PART}ID == -1) {
	if (move{PART}ID == '{ID}') {
		move{PART}ID = -1;
		$('.insert{PART}').fadeOut(200, function(){$('.insert{PART}PlaceHolder').show();});
	} else if(move{PART}ID == -1) {
		move{PART}ID = '{ID}';
		$('.insert{PART}PlaceHolder').hide();
		$('.insert{PART}').fadeIn(200);
		
		$(this).parent().parent().parent().parent().prev('li').hide();
		$(this).parent().parent().parent().parent().next('li').hide();
				
	} else {
		$('.gui-icon-button-MOVE').removeClass('gui-icon-button-MOVE_ENABLED');
		$('#m{PART}i' + move{PART}ID).toggleClass('gui-icon-button-MOVE_ENABLED');
		move{PART}ID = '{ID}';
		
		$('.insert{PART}PlaceHolder').hide();
		$('.insert{PART}').fadeIn(200);
		
		
		$(this).parent().parent().parent().parent().prev('li > .insert{PART}').hide();
		$(this).parent().parent().parent().parent().next('li > .insert{PART}').hide();
		
		
	}
	$(this).toggleClass('gui-icon-button-MOVE_ENABLED');
}