# Repo page runbook

The GitHub page around the README — description, topics and social preview. The description and topics are a one-time `gh` edit; the social preview is the one manual handover step. The implementing token may lack the repository-administration scope, so a human runs the two `gh repo edit` commands if they fail with `403`.

## Description

The description is the README purpose sentence verbatim — no separately authored pitch. Set it with:

```bash
gh repo edit luiznaac/label-follower --description "Keep up with the record labels you follow on Spotify: label-follower finds new tracks they've released and builds a Spotify playlist out of them automatically, instead of you checking each label's page by hand."
```

## Topics

Five topics from the standard's closed lists: `kotlin` and `react` (stack), `browser` and `http` (surface), `spotify` (domain). Set them with:

```bash
gh repo edit luiznaac/label-follower --add-topic kotlin --add-topic react --add-topic browser --add-topic http --add-topic spotify
```

## Social preview (manual handover)

GitHub has no public API for the social preview, so a human uploads it once:

1. Crop `docs/assets/primary-screen.png` to 1280×640 (the card's exact size, at most 1 MB).
2. Open the repository on GitHub → Settings → Social preview → Edit → Upload an image.
3. Upload the crop and save.

The crop is a second use of the same screenshot; do not author a separate card. Re-crop it whenever the primary screen changes. Visual tier and public repos only.
