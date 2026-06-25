// ── Chat functionality ──
const chatMessages = document.getElementById('chatMessages');
const chatInput = document.getElementById('chatInput');
const sendBtn = document.getElementById('sendBtn');
const documentId = document.getElementById('documentId')?.value;

function scrollToBottom() {
  if (chatMessages) chatMessages.scrollTop = chatMessages.scrollHeight;
}

function addMessage(role, content, sources = []) {
  const wrap = document.createElement('div');
  wrap.className = `message ${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'message-avatar';
  avatar.textContent = role === 'user' ? 'You' : 'AI';

  const bubble = document.createElement('div');
  bubble.className = 'message-bubble';

  const contentDiv = document.createElement('div');
  contentDiv.innerHTML = content.replace(/\n/g, '<br>');
  bubble.appendChild(contentDiv);

  if (sources && sources.length > 0) {
    const sourcesDiv = document.createElement('div');
    sourcesDiv.className = 'message-sources';
    sourcesDiv.innerHTML = '<div style="font-weight:600;margin-bottom:6px;color:var(--green-700)">📄 Sources</div>';
    sources.forEach(s => {
      const item = document.createElement('div');
      item.className = 'source-item';
      item.innerHTML = `<span class="source-page">Page ${s.page}</span><span>${s.preview}</span>`;
      sourcesDiv.appendChild(item);
    });
    bubble.appendChild(sourcesDiv);
  }

  wrap.appendChild(avatar);
  wrap.appendChild(bubble);
  chatMessages.appendChild(wrap);
  scrollToBottom();
}

function showTyping() {
  const wrap = document.createElement('div');
  wrap.className = 'message assistant';
  wrap.id = 'typing-indicator';
  wrap.innerHTML = `
    <div class="message-avatar">AI</div>
    <div class="message-bubble">
      <div class="typing-indicator">
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
      </div>
    </div>`;
  chatMessages.appendChild(wrap);
  scrollToBottom();
}

function removeTyping() {
  const t = document.getElementById('typing-indicator');
  if (t) t.remove();
}

async function sendMessage(question) {
  const q = question || chatInput?.value?.trim();
  if (!q || !documentId) return;

  if (chatInput) chatInput.value = '';
  if (sendBtn) sendBtn.disabled = true;

  addMessage('user', q);
  showTyping();

  try {
    const formData = new FormData();
    formData.append('documentId', documentId);
    formData.append('question', q);

    const res = await fetch('/api/ask', { method: 'POST', body: formData });
    const data = await res.json();
    removeTyping();
    addMessage('assistant', data.answer, data.sources);
  } catch (e) {
    removeTyping();
    addMessage('assistant', 'Sorry, something went wrong. Please try again.');
  } finally {
    if (sendBtn) sendBtn.disabled = false;
    if (chatInput) chatInput.focus();
  }
}

if (sendBtn) {
  sendBtn.addEventListener('click', () => sendMessage());
}

if (chatInput) {
  chatInput.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });
  chatInput.addEventListener('input', () => {
    chatInput.style.height = 'auto';
    chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + 'px';
  });
}

// Suggestion buttons
document.querySelectorAll('.suggestion-btn').forEach(btn => {
  btn.addEventListener('click', () => sendMessage(btn.dataset.question));
});

scrollToBottom();

// ── Upload functionality ──
const uploadZone = document.getElementById('uploadZone');
const fileInput = document.getElementById('fileInput');
const uploadForm = document.getElementById('uploadForm');
const uploadProgress = document.getElementById('uploadProgress');
const uploadBtn = document.getElementById('uploadBtn');

if (uploadZone) {
  uploadZone.addEventListener('click', () => fileInput?.click());

  uploadZone.addEventListener('dragover', e => {
    e.preventDefault();
    uploadZone.classList.add('dragover');
  });

  uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('dragover'));

  uploadZone.addEventListener('drop', e => {
    e.preventDefault();
    uploadZone.classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (file && fileInput) {
      const dt = new DataTransfer();
      dt.items.add(file);
      fileInput.files = dt.files;
      updateFilePreview(file);
    }
  });
}

if (fileInput) {
  fileInput.addEventListener('change', () => {
    if (fileInput.files[0]) updateFilePreview(fileInput.files[0]);
  });
}

function updateFilePreview(file) {
  const preview = document.getElementById('filePreview');
  const fileName = document.getElementById('fileName');
  const fileSize = document.getElementById('fileSize');
  if (preview) preview.style.display = 'flex';
  if (fileName) fileName.textContent = file.name;
  if (fileSize) fileSize.textContent = formatBytes(file.size);
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

if (uploadForm) {
  uploadForm.addEventListener('submit', () => {
    if (uploadProgress) uploadProgress.style.display = 'block';
    if (uploadBtn) {
      uploadBtn.disabled = true;
      uploadBtn.textContent = 'Processing...';
    }
  });
}

// ── Auto-dismiss alerts ──
document.querySelectorAll('.alert').forEach(alert => {
  setTimeout(() => {
    alert.style.opacity = '0';
    alert.style.transition = 'opacity 0.5s';
    setTimeout(() => alert.remove(), 500);
  }, 4000);
});
