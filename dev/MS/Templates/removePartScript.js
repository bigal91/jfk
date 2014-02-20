if (confirm('Do you really want to remove the {PART} with the id {LOCALID} from the questionnaire? This action cannot be undone!')) {
	ajax({
		url: ("{$BASEURL$}/survey/delete?part={PART}&{PART}ID={ID}"),			
		type: "GET",
		success:
			function (result) {
				$('#{PART}{ID}').fadeOut('500', function() {
					location.reload();
				});
			}
	});
}