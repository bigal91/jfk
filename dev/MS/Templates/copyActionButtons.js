	          		$('.buttonCell').html($('#columnButtons' + id).html());
	          		$('.buttonCell a').each(function(index) {
		          		$(this).attr('class', $(this).attr('class') + "_BIG");
		          		if ($(this).attr('style')) {
		          			$(this).attr('style', $(this).attr('style').replace('left', 'right'));
		          		}
	          		});