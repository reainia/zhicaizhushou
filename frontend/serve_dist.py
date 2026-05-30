"""HTTP 服务器：提供静态文件 + 代理 API 到 Java 后端"""
import http.server
import json
import os
import urllib.request
import urllib.error

PORT = 5174
DIST_DIR = os.path.join(os.path.dirname(__file__), 'dist')
JAVA_BACKEND = 'http://localhost:8080'


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST_DIR, **kwargs)

    def translate_path(self, path):
        if path.startswith('/finance/'):
            path = path[len('/finance'):]
        elif path in ('/finance', '/finance'):
            path = '/'
        return super().translate_path(path)

    def _proxy_request(self, method):
        """代理 API 请求到 Java 后端"""
        target = JAVA_BACKEND + self.path
        body = None
        content_len = int(self.headers.get('Content-Length', 0))
        if content_len > 0:
            body = self.rfile.read(content_len)

        req = urllib.request.Request(target, data=body, method=method)
        # 转发关键请求头
        for header in ('Content-Type', 'Authorization', 'Accept', 'Origin'):
            val = self.headers.get(header)
            if val:
                req.add_header(header, val)

        try:
            resp = urllib.request.urlopen(req)
            data = resp.read()
            self.send_response(resp.status)
            # 转发响应头（排除 transfer-encoding/chunked 避免冲突）
            for key, val in resp.headers.items():
                if key.lower() not in ('transfer-encoding', 'content-encoding', 'content-length'):
                    self.send_header(key, val)
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except urllib.error.HTTPError as e:
            data = e.read()
            self.send_response(e.code)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    def _is_api_path(self):
        return self.path.startswith('/api/')

    def do_GET(self):
        if self._is_api_path():
            return self._proxy_request('GET')
        return self._handle_static()

    def do_POST(self):
        if self._is_api_path():
            return self._proxy_request('POST')
        return self._handle_static()

    def do_PUT(self):
        if self._is_api_path():
            return self._proxy_request('PUT')
        return self._handle_static()

    def do_DELETE(self):
        if self._is_api_path():
            return self._proxy_request('DELETE')
        return self._handle_static()

    def do_OPTIONS(self):
        if self._is_api_path():
            self.send_response(204)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
            self.send_header('Access-Control-Allow-Headers', '*')
            self.send_header('Access-Control-Max-Age', '86400')
            self.end_headers()
            return
        return self._handle_static()

    def _handle_static(self):
        original_path = self.path
        # SPA fallback for /finance/* routes
        if original_path.startswith('/finance/'):
            if '.' not in original_path.rsplit('/', 1)[-1]:
                self.path = '/'
        elif original_path in ('/finance', '/finance'):
            self.path = '/'
        # /knowledge.html -> /chat-widget/knowledge.html
        elif original_path == '/knowledge.html':
            self.path = '/chat-widget/knowledge.html'
        return super().do_GET()


if __name__ == '__main__':
    server = http.server.HTTPServer(('0.0.0.0', PORT), Handler)
    print(f'Serving at http://localhost:{PORT}')
    print(f'  Vue app:   http://localhost:{PORT}/finance/')
    print(f'  API proxy:  /api/* -> {JAVA_BACKEND}/api/*')
    print(f'  Chat JS:    http://localhost:{PORT}/chat-widget/src/index.js')
    print(f'  Knowledge:  http://localhost:{PORT}/knowledge.html')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()
