runserver:
	cd epiccastle.io && python -m CGIHTTPServer 8000

deploy:
	rsync -av epiccastle.io/ root@epiccastle.io:/var/www/epiccastle.io/public/
	ssh root@epiccastle.io chown -R www-data:www-data /var/www/epiccastle.io
	ssh root@epiccastle.io service uwsgi restart
