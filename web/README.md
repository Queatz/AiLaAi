# Ai là ai

Ai là ai is a collaboration platform that helps you discover and stay connected to your city, enabling you to do more, externalize all of your visions, and go farther than you ever imagined. Think of it as a home page for your city!

## Prerequesites

The [`ailaai-shared`](https://github.com/Queatz/ailaai-shared) respository should be cloned as a sibling to this `ailaai-web` repository.

## Contributing

Built with [Compose for Web](https://jb.gg/compose-web) by [Jetbrains](https://www.jetbrains.com/).

Here's a quick primer:

- Every times state changes, everything gets re-executed.
- Anything inside a `remember { }` block will not be re-executed (i.e. it will be remembered) on re-composition.
- Only changes to `MutableState` (i.e. `mutableStateOf`) are able to trigger a re-composition.

## Run (development)

`./gradlew jsBrowserRun --continuous`

## Run

`./gradlew jsBrowserRun`

## Build

`./gradlew jsBrowserProductionWebpack`

Files are in `build/distributions`

## Deploy

```shell
apt update
apt install certbot nodejs npm nginx python3-certbot-nginx
```

### HTTP → HTTPS

1. Configure Nginx

2. Replace the contents of `/etc/nginx/sites-enabled/default` with the following

```
server {
    server_name <enter server host>;
    root /root/ui;
    listen 80;

    location / {
        index index.html;
        try_files $uri $uri/ /index.html =404;
    }
}
```

`chmod 755 /root`

3. Finally

```shell
certbot --nginx
nginx -t
service nginx restart
```
