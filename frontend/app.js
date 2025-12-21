{\rtf1\ansi\ansicpg936\cocoartf2867
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\margl1440\margr1440\vieww11520\viewh8400\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural\partightenfactor0

\f0\fs24 \cf0 document.getElementById("predictForm").addEventListener("submit", async (e) => \{\
  e.preventDefault();\
\
  const form = e.target;\
  const payload = \{\
    step: Number(form.step.value),\
    type_code: Number(form.type_code.value),\
    amount: Number(form.amount.value),\
    oldbalanceOrg: Number(form.oldbalanceOrg.value),\
    newbalanceOrig: Number(form.newbalanceOrig.value),\
    oldbalanceDest: Number(form.oldbalanceDest.value),\
    newbalanceDest: Number(form.newbalanceDest.value),\
    balanceDiffOrg: Number(form.balanceDiffOrg.value),\
    balanceDiffDest: Number(form.balanceDiffDest.value),\
  \};\
\
  const resultEl = document.getElementById("result");\
  resultEl.textContent = "Loading...";\
\
  try \{\
    const resp = await fetch("http://127.0.0.1:5001/api/predict", \{\
      method: "POST",\
      headers: \{"Content-Type": "application/json"\},\
      body: JSON.stringify(payload),\
    \});\
\
    const data = await resp.json();\
    resultEl.textContent = JSON.stringify(data, null, 2);\
  \} catch (err) \{\
    resultEl.textContent = String(err);\
  \}\
\});\
}