var isInvisible = $('#question{ID}').hasClass('invisibleQuestion');
ajax({
	url: ("{$BASEURL$}/survey/update?part=question&questionID={ID}&visibility=" + (isInvisible)),			
	type: "GET",
	success:
		function (result) {
			if($('#question{ID}').hasClass('invisibleQuestion')) {
				$('#question{ID}').removeClass('invisibleQuestion');
			} else {
				$('#question{ID}').addClass('invisibleQuestion');
			}
		}
});