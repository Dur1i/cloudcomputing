function autoGrowTextarea(textarea) {
    textarea.style.height = "auto";
    textarea.style.height = textarea.scrollHeight + "px";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function previewMedia(event) {
    const file = event.target.files[0];
    const container = document.getElementById("mediaPreviewContainer");
    const image = document.getElementById("imagePreview");
    const video = document.getElementById("videoPreview");
    if (!file || !container || !image || !video) return;

    const url = URL.createObjectURL(file);
    container.style.display = "block";

    if (file.type.startsWith("image/")) {
        image.src = url;
        image.style.display = "block";
        video.style.display = "none";
        video.removeAttribute("src");
    } else if (file.type.startsWith("video/")) {
        video.src = url;
        video.style.display = "block";
        image.style.display = "none";
        image.removeAttribute("src");
    }
}

function removeMedia() {
    const upload = document.getElementById("imageUpload");
    const container = document.getElementById("mediaPreviewContainer");
    const image = document.getElementById("imagePreview");
    const video = document.getElementById("videoPreview");
    if (upload) upload.value = "";
    if (container) container.style.display = "none";
    if (image) image.removeAttribute("src");
    if (video) video.removeAttribute("src");
}

function previewStoryMedia(event) {
    const file = event.target.files[0];
    const container = document.getElementById("storyPreviewContainer");
    const image = document.getElementById("storyImagePreview");
    const video = document.getElementById("storyVideoPreview");
    if (!file || !container || !image || !video) return;

    const url = URL.createObjectURL(file);
    container.style.display = "block";

    if (file.type.startsWith("video/")) {
        video.src = url;
        video.style.display = "block";
        image.style.display = "none";
        image.removeAttribute("src");
    } else {
        image.src = url;
        image.style.display = "block";
        video.style.display = "none";
        video.removeAttribute("src");
    }
}

function resetStoryPreview() {
    const upload = document.getElementById("storyFile");
    const container = document.getElementById("storyPreviewContainer");
    const image = document.getElementById("storyImagePreview");
    const video = document.getElementById("storyVideoPreview");
    if (upload) upload.value = "";
    if (container) container.style.display = "none";
    if (image) image.removeAttribute("src");
    if (video) {
        video.pause();
        video.removeAttribute("src");
    }
}

function applyStoredTheme() {
    let savedTheme = "light";
    try {
        savedTheme = localStorage.getItem("tc-theme") || "light";
    } catch (error) {
        savedTheme = "light";
    }

    document.body.dataset.theme = savedTheme;
    document.documentElement.classList.toggle("dark", savedTheme === "dark");
}

function toggleTheme() {
    const nextTheme = document.body.dataset.theme === "dark" ? "light" : "dark";
    document.body.dataset.theme = nextTheme;
    document.documentElement.classList.toggle("dark", nextTheme === "dark");
    try {
        localStorage.setItem("tc-theme", nextTheme);
    } catch (error) {
        // Keep the current-page theme even if storage is blocked.
    }
    showToast(nextTheme === "dark" ? "Dark mode on" : "Light mode on", "info");
}

function showToast(message, type = "info") {
    let stack = document.getElementById("toastStack");
    if (!stack) {
        stack = document.createElement("div");
        stack.id = "toastStack";
        stack.className = "toast-stack";
        document.body.appendChild(stack);
    }

    const toast = document.createElement("div");
    toast.className = "app-toast " + type;
    toast.setAttribute("role", type === "error" ? "alert" : "status");
    toast.setAttribute("aria-live", type === "error" ? "assertive" : "polite");
    toast.innerHTML = `<span>${escapeHtml(message)}</span><button type="button" aria-label="Close">&times;</button>`;
    toast.querySelector("button").addEventListener("click", () => toast.remove());
    stack.appendChild(toast);

    setTimeout(() => {
        toast.classList.add("leaving");
        setTimeout(() => toast.remove(), 180);
    }, 2600);
}

document.addEventListener("DOMContentLoaded", () => {
    applyStoredTheme();
    clearLegacyServiceWorkerCache();
    enhanceSubmittingForms();
    setupModalControls();
    setTimeout(() => document.body.classList.add("app-ready"), 120);
});

function clearLegacyServiceWorkerCache() {
    if ("serviceWorker" in navigator) {
        navigator.serviceWorker.getRegistrations()
            .then((registrations) => registrations.forEach((registration) => registration.unregister()))
            .catch(() => {});
    }
    if ("caches" in window) {
        caches.keys()
            .then((keys) => keys.filter((key) => key.startsWith("tc-social")).forEach((key) => caches.delete(key)))
            .catch(() => {});
    }
}

function enhanceSubmittingForms() {
    document.querySelectorAll("form").forEach((form) => {
        if (form.dataset.enhancedSubmit === "true") return;
        form.dataset.enhancedSubmit = "true";
        form.addEventListener("submit", () => {
            const inlineSubmit = form.getAttribute("onsubmit") || "";
            if (form.classList.contains("comment-form") || inlineSubmit.includes("submitComment") || form.dataset.noLoading === "true") return;
            const submitter = form.querySelector('button[type="submit"], .ui-button-primary');
            if (!submitter || submitter.dataset.noLoading === "true") return;
            submitter.setAttribute("aria-busy", "true");
            submitter.classList.add("is-loading");
            submitter.dataset.originalText = submitter.innerHTML;
            submitter.innerHTML = `<span class="button-spinner" aria-hidden="true"></span><span>Working...</span>`;
            setTimeout(() => {
                submitter.disabled = true;
            }, 0);
        });
    });
}

function setupModalControls() {
    if (document.body.dataset.modalControls === "ready") return;
    document.body.dataset.modalControls = "ready";

    document.addEventListener("click", (event) => {
        const toggle = event.target.closest('[data-bs-toggle="modal"]');
        if (toggle) {
            const selector = toggle.getAttribute("data-bs-target");
            const modal = selector ? document.querySelector(selector) : null;
            if (modal) openModal(modal);
        }

        const dismiss = event.target.closest('[data-bs-dismiss="modal"]');
        if (dismiss) {
            const modal = dismiss.closest(".modal");
            if (modal) closeModal(modal);
        }
    });

    document.addEventListener("mousedown", (event) => {
        if (event.target.classList && event.target.classList.contains("modal")) {
            closeModal(event.target);
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        const open = document.querySelector(".modal.flex, .modal.show");
        if (open) closeModal(open);
    });
}

function openModal(modal) {
    modal.classList.remove("hidden");
    modal.classList.add("flex", "show");
    modal.setAttribute("aria-hidden", "false");

    const dialog = modal.querySelector(".modal-dialog");
    if (dialog) {
        dialog.classList.remove("scale-95", "opacity-0");
        dialog.classList.add("scale-100", "opacity-100");
    }
}

function closeModal(modal) {
    const dialog = modal.querySelector(".modal-dialog");
    if (dialog) {
        dialog.classList.remove("scale-100", "opacity-100");
        dialog.classList.add("scale-95", "opacity-0");
    }

    setTimeout(() => {
        if (modal.id === "storyViewerModal") {
            const media = document.getElementById("storyViewerMedia");
            if (media) media.innerHTML = "";
            activeStoryId = null;
        }
        if (modal.id === "createStoryModal") resetStoryPreview();
        modal.classList.remove("flex", "show");
        modal.classList.add("hidden");
        modal.setAttribute("aria-hidden", "true");
    }, 120);
}

function toggleCommentSection(postId) {
    const section = document.getElementById("comment-section-" + postId);
    if (!section) return;
    section.style.display = section.style.display === "none" || !section.style.display ? "block" : "none";
}

function getPostElement(postId) {
    return document.getElementById("post-" + postId);
}

function setLikeVisual(button, liked) {
    const icon = button.querySelector("i");
    button.classList.toggle("liked", liked);
    button.classList.toggle("text-primary", liked);
    button.classList.toggle("dark:text-rose-500", liked);
    button.classList.toggle("text-slate-500", !liked);
    button.classList.toggle("dark:text-slate-400", !liked);
    if (icon) {
        icon.className = liked
            ? "fa-solid fa-heart"
            : "fa-regular fa-heart";
    }
}

function likePost(postId, button) {
    const countEl = button.querySelector('[data-role="like-count"]');
    const currentCount = Number.parseInt(countEl?.innerText || "0", 10);
    const wasLiked = button.classList.contains("liked") || button.classList.contains("text-primary");
    setLikeVisual(button, !wasLiked);
    if (countEl) countEl.innerText = String(Math.max(0, currentCount + (wasLiked ? -1 : 1)));

    fetch("/api/like/" + postId, { method: "POST" })
        .then((res) => res.json())
        .then((data) => {
            if (data.status === "error") {
                setLikeVisual(button, wasLiked);
                if (countEl) countEl.innerText = String(currentCount);
                showToast("Please log in again", "error");
                return;
            }
            setLikeVisual(button, !!data.liked);
            if (countEl) countEl.innerText = String(data.likeCount);
        })
        .catch(() => {
            setLikeVisual(button, wasLiked);
            if (countEl) countEl.innerText = String(currentCount);
            showToast("Could not update like", "error");
        });
}

function renderComment(comment) {
    const avatar = comment.avatarUrl || "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png";
    const username = escapeHtml(comment.username || "");
    const fullName = escapeHtml(comment.fullName || "User");
    const content = escapeHtml(comment.content || "");
    return `
        <div class="mb-3 d-flex gap-2" data-comment-id="${escapeHtml(comment.id)}">
            <img src="${escapeHtml(avatar)}" class="avatar avatar-sm" alt="avatar">
            <div class="comment-bubble">
                <a href="/profile/${username}" class="fw-bold text-decoration-none">${fullName}</a>
                <div class="text-break">${content}</div>
            </div>
        </div>`;
}

function submitComment(event, form) {
    submitCommentOptimistic(event, form);
}

function submitCommentOptimistic(event, form) {
    event.preventDefault();
    const data = new FormData(form);
    const postId = data.get("postId");
    const content = String(data.get("content") || "").trim();
    if (!postId || !content) return;

    fetch("/api/comment", { method: "POST", body: data })
        .then((res) => res.json())
        .then((payload) => {
            if (payload.status !== "created") {
                showToast("Could not add comment", "error");
                return;
            }
            const list = document.getElementById("comment-list-" + postId);
            if (list && payload.comment && !list.querySelector(`[data-comment-id="${payload.comment.id}"]`)) {
                list.insertAdjacentHTML("beforeend", renderComment(payload.comment));
            }
            const post = getPostElement(postId);
            const count = post?.querySelector('[data-role="comment-count"]');
            if (count) count.innerText = String(payload.commentCount);
            form.reset();
            showToast("Comment added", "success");
        })
        .catch(() => showToast("Could not add comment", "error"));
}

function deletePost(postId) {
    if (!confirm("Delete this post?")) return;
    fetch("/api/posts/delete/" + postId, { method: "DELETE" })
        .then((res) => {
            if (!res.ok) throw new Error("delete_failed");
            getPostElement(postId)?.remove();
            showToast("Post deleted", "success");
        })
        .catch(() => showToast("Could not delete post", "error"));
}

function renderPostHTML(post) {
    const mediaUrl = post.mediaUrl || "";
    const ext = mediaUrl.split(".").pop().toLowerCase();
    const isImage = ["jpg", "jpeg", "png", "gif", "webp"].includes(ext);
    const isVideo = ["mp4", "webm", "ogg"].includes(ext);
    const mediaHtml = mediaUrl && isImage
        ? `<div class="mt-3"><img src="${escapeHtml(mediaUrl)}" class="post-media" alt="post media"></div>`
        : mediaUrl && isVideo
            ? `<div class="mt-3"><video src="${escapeHtml(mediaUrl)}" class="post-media" controls></video></div>`
            : "";

    const canDelete = post.userId === window.currentUserId || window.currentUserRole === "ADMIN";
    const deleteButton = canDelete
        ? `<button type="button" class="action-link danger" onclick="deletePost(${post.id})" aria-label="Delete post"><i class="fa-solid fa-trash"></i></button>`
        : "";

    const comments = Array.isArray(post.comments) ? post.comments.map(renderComment).join("") : "";
    const liked = !!post.liked;
    const likeClasses = liked ? " liked" : "";
    const likeIcon = liked ? "fa-solid fa-heart" : "fa-regular fa-heart";

    return `
        <article class="post-card elevated-card" id="post-${escapeHtml(post.id)}">
            <div class="d-flex gap-3">
                <img src="${escapeHtml(post.avatarUrl || "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png")}" class="avatar" alt="avatar">
                <div class="w-100 min-w-0">
                    <div class="d-flex justify-content-between gap-2">
                        <div class="min-w-0">
                            <a href="/profile/${escapeHtml(post.username || "")}" class="post-author text-truncate d-inline-block">${escapeHtml(post.fullName || "User")}</a>
                            <span class="text-muted small">@${escapeHtml(post.username || "")}</span>
                        </div>
                        ${deleteButton}
                    </div>
                    <div class="mt-2 text-break post-body">${post.content || ""}</div>
                    ${mediaHtml}
                    <div class="post-actions">
                        <button type="button" class="action-link" onclick="toggleCommentSection(${post.id})">
                            <i class="fa-regular fa-comment"></i>
                            <span data-role="comment-count">${post.commentCount || 0}</span>
                        </button>
                        <button type="button" class="action-link danger${likeClasses}" onclick="likePost(${post.id}, this)">
                            <i class="${likeIcon}"></i>
                            <span data-role="like-count">${post.likeCount || 0}</span>
                        </button>
                    </div>
                    <div id="comment-section-${escapeHtml(post.id)}" style="display: none;" class="comment-panel">
                        <div class="comment-list" id="comment-list-${escapeHtml(post.id)}">${comments}</div>
                        <form action="/post/comment" method="POST" class="comment-form d-flex gap-2" onsubmit="submitComment(event, this)">
                            <input type="hidden" name="postId" value="${escapeHtml(post.id)}">
                            <input type="text" name="content" class="form-control modern-input" placeholder="Write a thoughtful reply..." required>
                            <button type="submit" class="ui-button ui-button-primary" data-no-loading="true">Reply</button>
                        </form>
                    </div>
                </div>
            </div>
        </article>`;
}

function subscribeToTopic(topic, callback) {
    window.tcSocket = window.tcSocket || { client: null, connected: false, connecting: false, subs: {}, pending: [] };
    const state = window.tcSocket;

    function subscribe(client) {
        if (state.subs[topic]) state.subs[topic].unsubscribe();
        state.subs[topic] = client.subscribe(topic, (message) => {
            callback(JSON.parse(message.body));
        });
    }

    if (state.connected && state.client) {
        subscribe(state.client);
        return;
    }

    state.pending.push({ topic, callback });
    if (state.connecting) return;
    if (!window.SockJS || !window.Stomp) return;
    state.connecting = true;
    const socket = new SockJS("/ws");
    state.client = Stomp.over(socket);
    state.client.debug = null;
    state.client.connect({}, () => {
        state.connected = true;
        state.connecting = false;
        const pending = state.pending.splice(0);
        pending.forEach((item) => subscribeToTopic(item.topic, item.callback));
    }, () => {
        state.connected = false;
        state.connecting = false;
        state.client = null;
    });
}

let activeStoryId = null;

function openStory(button) {
    activeStoryId = button.dataset.storyId;
    const avatar = document.getElementById("storyViewerAvatar");
    const name = document.getElementById("storyViewerName");
    const caption = document.getElementById("storyViewerCaption");
    const deleteButton = document.getElementById("storyDeleteBtn");
    const media = document.getElementById("storyViewerMedia");
    if (!media) return;

    if (avatar) avatar.src = button.dataset.storyAvatar || "";
    if (name) name.innerText = button.dataset.storyName || "Story";
    if (caption) caption.innerText = button.dataset.storyCaption || "";
    if (deleteButton) deleteButton.style.display = button.dataset.storyOwner === "true" ? "flex" : "none";

    media.innerHTML = button.dataset.storyType === "video"
        ? `<video src="${escapeHtml(button.dataset.storyMedia || "")}" controls autoplay></video>`
        : `<img src="${escapeHtml(button.dataset.storyMedia || "")}" alt="story">`;

    const modal = document.getElementById("storyViewerModal");
    if (modal) openModal(modal);
}

function deleteStory() {
    if (!activeStoryId || !confirm("Delete this story?")) return;
    fetch("/stories/delete/" + activeStoryId, { method: "POST" })
        .then((res) => res.text())
        .then((data) => {
            if (data !== "deleted") throw new Error("delete_failed");
            document.querySelector(`[data-story-id="${activeStoryId}"]`)?.remove();
            const modal = document.getElementById("storyViewerModal");
            if (modal) closeModal(modal);
            showToast("Story deleted", "success");
        })
        .catch(() => showToast("Could not delete story", "error"));
}
