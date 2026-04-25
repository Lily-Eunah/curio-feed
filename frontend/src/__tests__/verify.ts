/**
 * Lightweight structural verification — confirms key behavioral invariants
 * without a full test runner. Run with: npx tsx src/__tests__/verify.ts
 */

import { ARTICLES } from '../data/articles';

let passed = 0;
let failed = 0;

function assert(condition: boolean, label: string) {
  if (condition) {
    console.log(`  ✓ ${label}`);
    passed++;
  } else {
    console.error(`  ✗ ${label}`);
    failed++;
  }
}

// ── Data integrity ─────────────────────────────────────────────────────────────
console.log('\nData integrity:');
assert(ARTICLES.length === 8, 'has 8 articles');
ARTICLES.forEach(a => {
  assert(!!a.id && !!a.title && !!a.excerpt, `${a.id}: required fields present`);
  assert(a.vocabulary.length === 5, `${a.id}: has 5 vocabulary entries`);
  assert(!!a.quiz.q1 && !!a.quiz.q2 && !!a.quiz.q3, `${a.id}: has all 3 quiz questions`);
  assert(a.quiz.q1.options.length === 4, `${a.id}: q1 has 4 options`);
  assert(a.quiz.q2.options.length === 4, `${a.id}: q2 has 4 options`);
  assert(a.quiz.q1.correct >= 0 && a.quiz.q1.correct <= 3, `${a.id}: q1 correct index in range`);
  assert(a.quiz.q2.correct >= 0 && a.quiz.q2.correct <= 3, `${a.id}: q2 correct index in range`);
  assert(a.body.includes('{{'), `${a.id}: body has vocab tokens`);
});

// ── Category coverage ──────────────────────────────────────────────────────────
console.log('\nCategory coverage:');
const categories = new Set(ARTICLES.map(a => a.category));
['Tech', 'Science', 'Business', 'Culture'].forEach(c => {
  assert(categories.has(c), `category "${c}" has at least one article`);
});

// ── Vocab tokens match vocabulary list ────────────────────────────────────────
console.log('\nVocab token alignment:');
ARTICLES.forEach(a => {
  const tokens = [...a.body.matchAll(/{{([^}]+)}}/g)].map(m => m[1].toLowerCase());
  const vocabWords = new Set(a.vocabulary.map(v => v.word.toLowerCase()));
  tokens.forEach(t => {
    assert(vocabWords.has(t), `${a.id}: token "{{${t}}}" has matching vocab entry`);
  });
});

console.log(`\n${passed} passed, ${failed} failed\n`);
if (failed > 0) process.exit(1);
