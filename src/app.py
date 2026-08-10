"""Minimal HTTP service for the GitHub Actions CI/CD POC."""

from http.server import BaseHTTPRequestHandler, HTTPServer
import os


class RequestHandler(BaseHTTPRequestHandler):
    """Serve a health endpoint and a simple welcome message."""

    def do_GET(self) -> None:
        if self.path == "/health":
            body = b'{"status":"ok"}'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
        else:
            body = b"Python GitHub Actions CI/CD Demo\\n"
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")

        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        """Keep the POC output small."""
        return


def main() -> None:
    port = int(os.getenv("PORT", "3000"))
    server = HTTPServer(("0.0.0.0", port), RequestHandler)
    print(f"Python server running on port {port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
