/* =========================================================
   AI 客服聊天组件 — Web Component (Vanilla JS)
   使用 Shadow DOM，零外部依赖，无需构建步骤
   ========================================================= */

import styles from './styles.js';

const CHAT_ICON = `<svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/><path d="M7 9h10v2H7zm0-3h10v2H7z"/></svg>`;
const SEND_ICON  = `<svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>`;
const CLOSE_ICON = `<svg width="18" height="18" viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" fill="currentColor"/></svg>`;

class ChatWidget extends HTMLElement {
  constructor() {
    super();
    this._sessionId = null;
    this._isStreaming = false;
    this._abortController = null;

    this.attachShadow({ mode: 'open' });
    this.shadowRoot.innerHTML = `
      <style>${styles}</style>

      <button id="trigger" aria-label="打开客服聊天" part="trigger">
        ${CHAT_ICON}
      </button>

      <div id="panel">
        <div id="header">
          <span id="header-title">${this.getAttribute('title') || 'AI 客服'}</span>
          <button id="header-close" aria-label="关闭">${CLOSE_ICON}</button>
        </div>
        <div id="messages" role="log" aria-live="polite"></div>
        <div id="input-area">
          <textarea
            id="input"
            rows="1"
            placeholder="请输入您的问题..."
            aria-label="输入消息"
          ></textarea>
          <button id="send-btn" disabled aria-label="发送">${SEND_ICON}</button>
        </div>
      </div>
    `;

    this._trigger  = this.shadowRoot.getElementById('trigger');
    this._panel    = this.shadowRoot.getElementById('panel');
    this._closeBtn = this.shadowRoot.getElementById('header-close');
    this._messages = this.shadowRoot.getElementById('messages');
    this._input    = this.shadowRoot.getElementById('input');
    this._sendBtn  = this.shadowRoot.getElementById('send-btn');

    this._bindEvents();
  }

  static get observedAttributes() {
    return ['server-url', 'title', 'theme-color'];
  }

  get serverUrl() {
    return this.getAttribute('server-url') || 'http://localhost:8000';
  }

  attributeChangedCallback(name, oldVal, newVal) {
    if (name === 'theme-color' && newVal) {
      this.style.setProperty('--widget-primary', newVal);
    }
    if (name === 'title' && this.shadowRoot) {
      const el = this.shadowRoot.getElementById('header-title');
      if (el) el.textContent = newVal;
    }
  }

  _bindEvents() {
    this._trigger.addEventListener('click', () => this.open());
    this._closeBtn.addEventListener('click', () => this.close());
    this._input.addEventListener('input', () => this._onInput());
    this._input.addEventListener('keydown', (e) => this._onKeydown(e));
    this._sendBtn.addEventListener('click', () => this._send());
  }

  open() {
    this._panel.classList.add('open');
    this._trigger.style.display = 'none';
    this._input.focus();
    if (!this._sessionId && this._messages.children.length === 0) {
      this._addMessage('assistant', '您好！我是 AI 客服助手，有什么可以帮您的吗？');
    }
  }

  close() {
    this._panel.classList.remove('open');
    this._trigger.style.display = 'flex';
  }

  _onInput() {
    this._sendBtn.disabled = !this._input.value.trim() || this._isStreaming;
    this._autoResize();
  }

  _autoResize() {
    this._input.style.height = 'auto';
    this._input.style.height = Math.min(this._input.scrollHeight, 120) + 'px';
  }

  _onKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this._send();
    }
  }

  async _send() {
    const text = this._input.value.trim();
    if (!text || this._isStreaming) return;

    this._input.value = '';
    this._input.style.height = 'auto';
    this._sendBtn.disabled = true;

    this._addMessage('user', text);
    this._addTyping();

    this._isStreaming = true;
    this._abortController = new AbortController();

    try {
      await this._streamChat(text);
    } catch (err) {
      if (err.name !== 'AbortError') {
        this._addError('请求失败，点击重试', () => this._send());
      }
    } finally {
      this._removeTyping();
      this._isStreaming = false;
      this._sendBtn.disabled = !this._input.value.trim();
    }
  }

  // ── 核心 SSE 流式请求 ──
  async _streamChat(message) {
    const url = `${this.serverUrl}/api/v1/chat/stream`;

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        session_id: this._sessionId,
        message,
      }),
      signal: this._abortController.signal,
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    // 创建消息气泡
    const msgEl = this._addMessage('assistant', '', true);

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let currentEvent = '';
    let currentData = '';

    function flushEvent(widget) {
      if (!currentEvent || currentData === undefined) return;
      widget._handleSSEEvent(currentEvent, currentData, msgEl);
      currentEvent = '';
      currentData = '';
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // 关键修复：SSE 使用 \r\n 换行，先统一转为 \n
      // 否则 split('\n\n') 无法匹配 \r\n\r\n 事件分隔符
      buffer = buffer.replace(/\r\n/g, '\n');

      // 按 SSE 双换行分割（事件分隔符）
      const parts = buffer.split('\n\n');
      // 最后一个可能是不完整的，留到下次
      buffer = parts.pop() || '';

      for (const part of parts) {
        const lines = part.split('\n');
        currentEvent = '';
        currentData = '';

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i];
          if (line.startsWith('event: ')) {
            currentEvent = line.slice(7).trim();
          } else if (line.startsWith('data: ')) {
            currentData = line.slice(6);
          }
        }

        if (currentEvent) {
          flushEvent(this);
        }
      }
    }

    // 处理缓冲区剩余内容
    if (buffer.trim()) {
      const lines = buffer.split('\n');
      for (const line of lines) {
        if (line.startsWith('event: ')) currentEvent = line.slice(7).trim();
        else if (line.startsWith('data: ')) currentData = line.slice(6);
      }
      if (currentEvent) flushEvent(this);
    }
  }

  _handleSSEEvent(type, data, msgEl) {
    switch (type) {
      case 'token':
        msgEl._content += data;
        msgEl.textContent = msgEl._content;
        this._scrollToBottom();
        break;
      case 'sources':
        try {
          const sources = JSON.parse(data);
          if (Array.isArray(sources) && sources.length > 0) {
            const names = [...new Set(sources.map(s => s.filename))];
            this._addMessage('sources', '参考: ' + names.join(', '));
          }
        } catch {}
        break;
      case 'done':
        // 可选：会话记录等
        break;
    }
  }

  _addMessage(role, content, isStreaming = false) {
    const el = document.createElement('div');
    el.className = 'msg ' + role;
    if (isStreaming) {
      el._content = '';
      el.textContent = '';
    } else {
      el.textContent = content;
    }
    this._messages.appendChild(el);
    this._scrollToBottom();
    return el;
  }

  _addTyping() {
    // 移除已有的 typing 动画
    this._removeTyping();
    const el = document.createElement('div');
    el.className = 'typing';
    el.id = '__typing__';
    el.innerHTML = '<span></span><span></span><span></span>';
    this._messages.appendChild(el);
    this._scrollToBottom();
  }

  _removeTyping() {
    const el = this._messages.querySelector('#__typing__');
    if (el) el.remove();
  }

  _addError(text, onClick) {
    const el = document.createElement('div');
    el.className = 'error-msg';
    el.textContent = text;
    if (onClick) el.addEventListener('click', onClick);
    this._messages.appendChild(el);
    this._scrollToBottom();
  }

  _scrollToBottom() {
    requestAnimationFrame(() => {
      this._messages.scrollTop = this._messages.scrollHeight;
    });
  }
}

if (!customElements.get('chat-widget')) {
  customElements.define('chat-widget', ChatWidget);
}

export default ChatWidget;
