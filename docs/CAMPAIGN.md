# Oh Heck! Campaign Outline

Final revision, 2026-11-23. Worked out in the [campaign outline doc](https://claude.ai/artifact/9YBc2MMojUx3GKHZxb9ye1).

## 1. Overview

The title screen splits into **Campaign** and **Custom game**. Campaign is a guided climb through chapters, each built on one game mode; Custom game is today's game, with every rule toggle.

- **Campaign:** chapters in order, each with table play, trick shots, a final table and a duel to pass.
- **Custom game:** pick a mode or set the rules yourself, as in Options today.
- Both share the roster, the rules engine and the bots; Campaign adds progress, unlocks and badges.

A new player's first Play asks "New to Oh Heck?": Teach me plays a four-hand practice round that teaches the rules as they come up, then turns into the Chapter 1 card and opens chapter 1's welcome, which explains what a chapter is (a table of players, their house rules and a champion) and lets them tap each player to read their habit. I know how to play goes straight to the Play menu.

## 2. Campaign structure

Each chapter is a table: a group of players, their house rules (its game mode) and a champion. It has four steps: win at the table, sharpen up with its trick shots, win a final table with the champion sitting in, then win a duel to move on.

1. **Table play:** win a full four-player game under the chapter's mode (section 5).
2. **Trick shots:** puzzles that teach the chapter's mode (section 4). Optional practice, open once the table is won.
3. **Final table:** the champion takes the top seat for a full game under the house rules plus the champion's duel twist (chapter 1: momentum). Open once the table is won; winning it opens the duel.
4. **Duel:** beat the chapter's champion one-on-one. Winning the duel opens the next chapter (section 6).

| Chapter | Mode | Table rivals | Duel champion |
| --- | --- | --- | --- |
| 1 | Classic | Edmund, Barnaby, Horatio | Nathaniel |
| 2 | High Stakes | Rupert, Cornelius, Oswald | Phillip |
| 3 | Poker Face | Virgil, Percival, Phillip | Mortimer |
| 4 | Marathon | the Hard players | Wilfred |

The rival line-ups are a first pass; the mode order runs from the most familiar game to the longest.

## 3. Campaign screen: the chapter hand

The campaign is shown as a hand of cards, one large card per chapter, fanned across the felt like your hand at the table.

- **A chapter card** shows its number, its mode (Classic, High Stakes, Poker Face, Marathon) and its champion's name.
- **Four pips** on each card stand for table, trick shots, final table and duel; a pip fills in gold when that step is done.
- **Locked chapters** lie face down, showing the card back. The chapter in progress is lifted out of the fan with the trump glow.
- **Tap a chapter** and its card flips and grows; its four steps lie on the felt as four cards, like a trick, each held back until the step before is won.
- Built from what the table already draws (card art, the fan, glints, the deal motion), so it looks like the game rather than a menu.

## 4. Trick shots

Trick shots are hand-built puzzles: every hand is face up, the deal is fixed, and there is a right line of play. The players are unnamed; each shows its bid with the table's bid icons. No stars and no undo: a restart deals the shot again.

- **Goals** rotate: *Make it* (make exactly your bid), *Block* (make your bid and stop the outlined player making theirs), *Choose the bid* (the one bid you can guarantee).
- **Teaching:** each chapter's puzzles use its mode, so they rehearse what the table and duel will ask (a big-bid gamble in chapter 2, reading blind bids in chapter 3).
- **Supply:** hand-written, three per chapter, one of each goal. No generator for now.
- Each puzzle is checked by a small solver in the tests, so every puzzle is solvable.

## 5. Table play

Table play is a normal four-player game under the chapter's mode; finishing first clears it and unlocks the duel.

- The three table rivals are fixed per chapter (section 2), so each chapter has its own cast.
- Lose and you simply play again; the rivals stay the same and the deal changes.
- It uses the existing game screen, rules and bots unchanged.

## 6. Duels

A duel is you against the chapter's champion, one-on-one; winning it is the only way into the next chapter.

- **Short deck:** 9 through Ace (24 cards), hands of 1 to 6 cards, so most of the deck is in play and counting matters.
- **Hidden hands:** the champion's cards are never shown; nothing in a duel reveals an opponent's hand.
- **Momentum:** make your bid twice running and the next hand scores x2.
- **Champion quirks:** the champion's style shows plainly, but only ever as a choice between sensible moves (Phillip takes the higher of two sound bids; Mortimer empties his shortest suit first when it costs nothing). A quirk never overrides common sense: no zero bid on the trump ace, no thrown hand.
- **Length:** best of five short hands, about 2 to 3 minutes.
- **Within the chapter's mode:** a High Stakes duel scores big bids, a Poker Face duel bids blind.
- **The entrance:** each duel opens with something showy for the champion, e.g. the chapter card flipping over and dealing itself into their seat. The exact form is left to the build.

**Duel variations**, so the four duels don't play alike:

| Variation | How it plays | Why it's fun |
| --- | --- | --- |
| Standard | points, with momentum | the baseline |
| Winner picks trump | whoever made their bid last hand chooses the trump suit once the next hand is dealt | a made bid earns a real choice, not just points |
| Hands won | the duel counts made bids, not points; a tie goes to sudden death, one hand of a single card | every hand is a must-make; ties are tense |
| The widow | two cards lie face down to the side; whoever made their bid last hand may swap one card from their hand for one of them before bidding | a made bid buys a chance to mend a bad hand |

Proposed: chapter 1 Standard, chapter 2 Winner picks trump, chapter 3 Hands won, chapter 4 The widow, with Marathon's up-and-down hand sizes.

Losing a duel costs nothing: you can retry it at once.

## 7. Unlockables and badges

Progress pays out in things you can see and use, and badges mark what you have done.

- **Unlockables:**
  - Each beaten champion joins Custom game as a duel opponent.
  - Each chapter cleared unlocks a card back and a table felt that represent that chapter. Every mode stays open in Custom game from the start; the campaign unlocks champions, card backs and felts, not modes.
- **Badges:** one for each champion beaten in a campaign duel, plus one-off feats, all shown on Records. Feat examples: *First Duel* (win any duel), *Clean Sweep* (make every bid in a game), *Big Spender* (make a bid of 5 or more under big bids), *Poker Face* (win a blind-bid game without a miss), *Marathoner* (finish a Marathon).
- Card backs, felts and badge art can be generated images, made outright rather than drawn by the art scripts.

## 8. Custom game

Custom game is today's game, unchanged: pick Classic, High Stakes, Poker Face or Marathon, or set every rule yourself.

- Duels join it as a mode once the first champion is beaten, with any unlocked champion as the opponent.
- High scores and statistics keep working as now.

## 9. MVP scope and build order

The MVP is the Campaign / Custom game split with chapter 1 playable end to end; each step below ships as its own dev build.

1. **Title split:** Campaign and Custom game buttons; Custom game is today's Play.
2. **Duel:** a two-player short-deck rule set with momentum, champion quirks and the four duel variations; bots learn two-player bidding and pass the arena.
3. **Campaign chapter 1:** the chapter hand screen, table play with fixed rivals, the duel gate, saved progress.
4. **Trick shots:** the puzzle screen, 3 Classic puzzles (one of each goal), and the solver test.
5. **Practice hand-off:** the practice round ends by opening chapter 1.
6. **Badges and unlockables:** the first badges on Records (including the chapter 1 champion's), chapter 1's card back and felt.
7. **Chapters 2 to 4.**

## 10. Decisions

Settled for the MVP.

- Rival line-ups per chapter: as in section 2.
- No undo in trick shots; three per chapter, one of each goal.
- A lost duel can be retried at once, at no cost.
- Every mode stays open in Custom game; the campaign unlocks champions, card backs and felts, not modes.
- Cosmetics: card backs and table felts, one of each per chapter, as generated images.
- Momentum scores only your own streak; breaking the champion's streak earns nothing.
- Champion quirks choose between sensible moves and never override common sense.
- Each champion beaten in a campaign duel earns a badge.
- New players learn in a practice round that leads into chapter 1; the old tutorial pages are gone.
- Nothing ever shows an opponent's cards. Ruled out: dialogue, dailies, hints, a map, a drafted deal, an open final hand, wagers that can be farmed, called cards, quirks that escalate, per-chapter house rules, a second sitting, badges on the placard, per-champion card backs, a campaign record page, a declared blind zero, moonshots, bowers, court powers, a trump auction, marriage bonuses, press on, the cloud, the bomb, the excuse, a champion's card, side prizes, a joker call.

## 11. Later

Agreed in principle, not in the MVP.

1. **Gauntlet:** after the last chapter, duels in a row against every champion; how far you get is a high score.
2. **Open stock:** a mode where the cards not dealt lie face up, so you know exactly what is out. Pulled from duels; it may make a good game mode of its own.
3. **Double as a power-up:** make your bid three hands running to earn one Double for the rest of the duel; play it after the first trick to double that hand, win or lose. The champion can earn one too.
4. **Bags in Marathon:** every trick over your bid is a bag, and every fifth bag costs 25 points. Worth trying where the game is long enough for bags to pile up.
5. **Jokers:** two jokers join the deck (54 cards); a turned-up joker means no trump that hand. Tap a joker and choose **Take** (beats every card, trumps included) or **Duck** (loses to every card); it may be played at any time, even when you could follow suit. Led, the next card played sets the suit. In one trick the first Take wins, and two Ducks leave it to the best ordinary card. A joker is a trick you can claim or refuse later, and the bots bid it that way. Only Take or Duck, because naming a suit and rank mid-trick is too fiddly on a phone.
