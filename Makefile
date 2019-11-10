runserver:
	cd epiccastle.io && python -m CGIHTTPServer 8000

deploy: build
	rsync -av \
		--exclude=templates \
		--exclude=*.clj \
		--exclude=*~ \
		epiccastle.io/ \
		root@epiccastle.io:/var/www/epiccastle.io/public/
	ssh root@epiccastle.io chown -R www-data:www-data /var/www/epiccastle.io
	ssh root@epiccastle.io service uwsgi restart

epiccastle.io/%.html: epiccastle.io/%.clj epiccastle.io/templates/site.html
	bootleg $< > $@

build: epiccastle.io/index.html epiccastle.io/contact.html
