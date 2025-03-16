#Server

nohup java -Dkotlin.script.classpath='Ai La Ai Backend-0.0.1-all.jar' -jar 'Ai La Ai Backend-0.0.1-all.jar' > log.txt 2> errors.txt < /dev/null &

```shell
apt update
apt install certbot nginx default-jre python3-certbot-nginx
```

See: https://arangodb.com/download-major/ubuntu/ for latest version of ArangoDB

```shell
echo 'deb https://download.arangodb.com/arangodb312/DEBIAN/ /' | sudo tee /etc/apt/sources.list.d/arangodb.list
apt install apt-transport-https
apt update
apt install arangodb3
```

File: `/etc/nginx/sites-enabled/default`

```conf                      
server {
    server_name api.ailaai.app; # managed by Certbot

    client_max_body_size 100m;

    keepalive_timeout 300s;
    send_timeout 300s;
    client_body_timeout 300s;
    client_header_timeout 300s;
    proxy_connect_timeout 60s;
    proxy_read_timeout 300s;
    proxy_send_timeout 300s;

        location / {
            proxy_pass http://localhost:8080;
            #proxy_http_version 1.1;
            proxy_set_header Connection "";
            chunked_transfer_encoding off;
            proxy_buffering off;
            proxy_cache off;
        }

    listen [::]:443 ssl http2 ipv6only=on; # managed by Certbot
    listen 443 ssl http2; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/api.ailaai.app/fullchain.pem; # manag>
    ssl_certificate_key /etc/letsencrypt/live/api.ailaai.app/privkey.pem; # man>
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
}

server {
    if ($host = api.ailaai.app) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


        listen 80 ;
        listen [::]:80 ;
    server_name api.ailaai.app;
    return 404; # managed by Certbot


}

```