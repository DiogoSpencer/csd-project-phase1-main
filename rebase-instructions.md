# Instructions to rebase on upstream

Commit all existing changes and make a backup of your main branch:
```
❯ git add --all && git commit -m "all changes so far"
...
❯ git checkout -b backup
Switched to a new branch 'backup'
❯ git checkout main
```

Check current remotes:
```
❯ git remote -v
origin	git@github.com:UNL-MEI-CSD/csd-project-phase-1-alxdavids.git (fetch)
origin	git@github.com:UNL-MEI-CSD/csd-project-phase-1-alxdavids.git (push)
```

Set up new remote:
```
❯ git remote add upstream git@github.com:UNL-MEI-CSD/csd-project-phase1.git
❯ git fetch upstream
...
```

Rebase on upstream branch:
```
git rebase upstream/main
```

SOLVE CONFLICTS and then (you may have to do this multiple times):
```
git add/rm <conflicted_files>
git rebase --continue
```

BEWARE, THE NEXT INSTRUCTION IS VERY RISKY AND CAN ERASE DATA.
THIS IS WHY IT IS IMPORTANT TO BACKUP YOUR OLD BRANCH. 
MAKE SURE YOU HAVE DONE THIS FIRST.

Force push main branch to `origin`:
```
git push -f origin main
```