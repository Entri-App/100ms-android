# Workflow: Syncing & Releasing entri-100ms

This guide explains how we keep our fork (**entri-100ms**) in sync with the upstream
[100ms](https://github.com/100mslive/100ms-android) repo and how we cut a release that
triggers a JitPack build for our app.

---

## Overview

1. Sync our fork's `release-v2` with upstream 100ms `release-v2`.
2. Create a new, version-bumped branch from the latest released branch.
3. Rebase it on top of the previously synced fork.
4. Push a tag to trigger a JitPack build (QA first, then release).

---

## 1. Sync the fork with upstream

Sync our fork **entri-100ms** `release-v2` with the upstream **100ms** `release-v2`.

After syncing, update your **local** `release-v2` branch so it has the latest changes:

```bash
git checkout release-v2
git pull origin release-v2
```

> Our `entri-100ms` code currently lives on the **`release-v10`** branch
> _(latest at the time of writing — always check the repo for the newest `release-vN` branch)_.

## 2. Create the next release branch

First, find the **latest release-versioned branch** in the repo (the highest `release-vN`).
At the time of writing this is `release-v10`. Then create a new branch from it,
**bumping the version by one**:

```
release-v10  ->  release-v11
```

> Replace `v10`/`v11` with the actual current and next numbers when you do this.

## 3. Rebase

Rebase the new branch on top of the earlier-synced fork (`entri-100ms release-v2`).

---

## 4. Build & Tag (JitPack)

To test on our app, we push a Git tag, which kicks off a JitPack build.

### Versioning convention

Our version = **upstream 100ms version + `.0`**

| Build type | Upstream version | Our tag           |
| ---------- | ---------------- | ----------------- |
| QA / beta  | `1.3.10`         | `1.3.10.0-beta01` |
| Release    | `1.3.10`         | `1.3.10.0`        |

**Rules**

- For each consecutive QA build, increment the beta number: `beta01` → `beta02` → `beta03` …
- The **final release build must not contain** any `beta` or `alpha` suffix.

### Push a QA (beta) build

```bash
git tag v1.3.10.0-beta01
git push origin v1.3.10.0-beta01
```

### Push the final release build

Once QA is approved, push the clean release tag:

```bash
git tag v1.3.10.0
git push origin v1.3.10.0
```
