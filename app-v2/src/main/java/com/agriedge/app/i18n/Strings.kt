package com.agriedge.app.i18n

import com.agriedge.app.config.AppConfig.Lang

/**
 * UI strings in English, Marathi and Hindi.
 * Marathi and Hindi should be reviewed by a native speaker before release.
 */
object Strings {

    private val rows: Map<String, Triple<String, String, String>> = mapOf(
        // Navigation & app
        "app_name" to Triple("AgriEdge", "अॅग्रीएज", "एग्रीएज"),
        "tab_home" to Triple("Home", "मुख्यपृष्ठ", "होम"),
        "tab_assistant" to Triple("Assistant", "सहाय्यक", "सहायक"),
        "tab_market" to Triple("Market", "बाजार", "बाज़ार"),

        // Home
        "greeting" to Triple("Good day, farmer", "नमस्कार, शेतकरी मित्रा", "नमस्ते, किसान भाई"),
        "home_sub" to Triple("Offline crop care for banana and papaya", "केळी आणि पपईसाठी ऑफलाइन पीक सल्ला", "केला और पपीते के लिए ऑफ़लाइन फसल सलाह"),
        "hero_title" to Triple("Check your leaf in seconds", "पानाची तपासणी काही सेकंदात", "पत्ती की जाँच कुछ सेकंडों में"),
        "hero_sub" to Triple("Point the camera at one leaf. Works without internet.", "एका पानावर कॅमेरा धरा. इंटरनेटशिवाय चालते.", "एक पत्ती पर कैमरा रखें। इंटरनेट के बिना चलता है।"),
        "take_photo" to Triple("Take photo", "फोटो काढा", "फोटो लें"),
        "choose_image" to Triple("Choose from gallery", "गॅलरीतून निवडा", "गैलरी से चुनें"),
        "model_loading" to Triple("Loading offline models…", "ऑफलाइन मॉडेल लोड होत आहेत…", "ऑफ़लाइन मॉडल लोड हो रहे हैं…"),
        "model_missing" to Triple("Model files missing. Copy them to the phone and restart the app.", "मॉडेल फाइल्स सापडल्या नाहीत. त्या फोनवर कॉपी करून अॅप पुन्हा सुरू करा.", "मॉडल फ़ाइलें नहीं मिलीं। फ़ोन में कॉपी करके ऐप दोबारा शुरू करें।"),
        "vision_not_ready" to Triple("Leaf scanner is not ready yet.", "पान स्कॅनर अजून तयार नाही.", "पत्ती स्कैनर अभी तैयार नहीं है।"),
        "district_label" to Triple("Your district", "तुमचा जिल्हा", "आपका ज़िला"),
        "district_suits" to Triple("Crops that suit your district", "तुमच्या जिल्ह्यासाठी योग्य पिके", "आपके ज़िले के लिए उपयुक्त फसलें"),
        "no_crops" to Triple("No crop data for this district yet.", "या जिल्ह्यासाठी अजून पीक माहिती नाही.", "इस ज़िले के लिए अभी फसल जानकारी नहीं है।"),
        "quick_assistant" to Triple("Ask the assistant", "सहाय्यकाला विचारा", "सहायक से पूछें"),
        "quick_assistant_sub" to Triple("Fertilizer, irrigation, pests", "खत, पाणी, कीड", "खाद, सिंचाई, कीट"),
        "quick_market" to Triple("Market and profit", "बाजार आणि नफा", "बाज़ार और मुनाफ़ा"),
        "quick_market_sub" to Triple("Mandi prices, per-acre profit", "मंडी भाव, प्रति एकर नफा", "मंडी भाव, प्रति एकड़ मुनाफ़ा"),

        // Crop names
        "crop_banana" to Triple("Banana", "केळी", "केला"),
        "crop_papaya" to Triple("Papaya", "पपई", "पपीता"),

        // Camera
        "camera_hint" to Triple("Fill the frame with one leaf", "एका पानाने फ्रेम भरा", "एक पत्ती से फ्रेम भरें"),
        "cancel" to Triple("Cancel", "रद्द करा", "रद्द करें"),
        "camera_permission" to Triple("Camera permission is needed to photograph leaves.", "पानांचे फोटो काढण्यासाठी कॅमेरा परवानगी हवी.", "पत्तियों की फोटो लेने के लिए कैमरा अनुमति चाहिए।"),
        "grant_permission" to Triple("Allow camera", "कॅमेरा परवानगी द्या", "कैमरा अनुमति दें"),

        // Analysis & result
        "analyzing" to Triple("Analysing leaf…", "पानाचे विश्लेषण सुरू…", "पत्ती का विश्लेषण हो रहा है…"),
        "classify_failed" to Triple("Could not analyse this photo", "हा फोटो तपासता आला नाही", "यह फोटो जाँची नहीं जा सकी"),
        "error_title" to Triple("Something went wrong", "काहीतरी चुकले", "कुछ गड़बड़ हुई"),
        "return_home" to Triple("Back to home", "मुख्यपृष्ठावर परत", "होम पर वापस"),
        "diagnosis" to Triple("Diagnosis", "निदान", "निदान"),
        "healthy" to Triple("Looks healthy", "निरोगी दिसते", "स्वस्थ लगता है"),
        "needs_attention" to Triple("Needs attention", "लक्ष देणे आवश्यक", "ध्यान देने की ज़रूरत"),
        "low_confidence" to Triple("Low confidence", "खात्री कमी", "विश्वास कम"),
        "low_conf_msg" to Triple("The app is not sure. Retake the photo in daylight, with one leaf filling the frame.", "अॅपला खात्री नाही. दिवसाच्या प्रकाशात, एका पानाने फ्रेम भरून पुन्हा फोटो काढा.", "ऐप को पक्का नहीं है। दिन की रोशनी में, एक पत्ती से फ्रेम भरकर दोबारा फोटो लें।"),
        "unsupported_title" to Triple("Outside the focus crops", "मुख्य पिकांबाहेरील", "मुख्य फसलों से बाहर"),
        "unsupported_msg" to Triple("Detailed guidance is currently available for banana and papaya only.", "सध्या केळी आणि पपईसाठीच सविस्तर मार्गदर्शन उपलब्ध आहे.", "फ़िलहाल केला और पपीता के लिए ही विस्तृत मार्गदर्शन उपलब्ध है।"),
        "confidence" to Triple("Confidence", "खात्री", "विश्वास"),
        "explanation" to Triple("What it means", "याचा अर्थ", "इसका मतलब"),
        "treatment" to Triple("What to do", "काय करावे", "क्या करें"),
        "ai_advice" to Triple("AI advice", "AI सल्ला", "AI सलाह"),
        "generating" to Triple("Generating advice…", "सल्ला तयार होत आहे…", "सलाह तैयार हो रही है…"),
        "stop" to Triple("Stop", "थांबवा", "रोकें"),
        "other_possibilities" to Triple("Other possibilities", "इतर शक्यता", "अन्य संभावनाएँ"),
        "scan_another" to Triple("Scan another leaf", "दुसरे पान तपासा", "एक और पत्ती जाँचें"),

        // Follow-up chat
        "ask_title" to Triple("Ask about this result", "या निदानाबद्दल विचारा", "इस निदान के बारे में पूछें"),
        "ask_sub" to Triple("Answers are generated on the phone.", "उत्तरे फोनवरच तयार होतात.", "जवाब फ़ोन पर ही बनते हैं।"),
        "ask_placeholder" to Triple("Type your question", "प्रश्न लिहा", "सवाल लिखें"),
        "fu_treat" to Triple("How to treat it?", "उपचार कसा करू?", "इलाज कैसे करूँ?"),
        "fu_organic" to Triple("Organic options?", "सेंद्रिय उपाय?", "जैविक उपाय?"),
        "fu_prevent" to Triple("How to prevent it?", "प्रतिबंध कसा करू?", "बचाव कैसे करूँ?"),
        "send" to Triple("Send", "पाठवा", "भेजें"),

        // Assistant tab
        "assistant_title" to Triple("Farm assistant", "शेती सहाय्यक", "खेती सहायक"),
        "assistant_sub" to Triple("Offline. Ask about banana, papaya, fertilizer, water or pests.", "ऑफलाइन. केळी, पपई, खत, पाणी किंवा कीड याबद्दल विचारा.", "ऑफ़लाइन। केला, पपीता, खाद, पानी या कीट के बारे में पूछें।"),
        "assistant_empty" to Triple("Ask a question to start", "सुरुवात करण्यासाठी प्रश्न विचारा", "शुरू करने के लिए सवाल पूछें"),
        "clear" to Triple("Clear", "साफ करा", "साफ़ करें"),
        "suggest_1" to Triple("Banana planting distance", "केळी लागवड अंतर", "केला रोपण दूरी"),
        "suggest_2" to Triple("Papaya fruit drop reasons", "पपई फळगळ कारणे", "पपीता फल गिरने के कारण"),
        "suggest_3" to Triple("Water tips for banana", "केळीसाठी पाणी नियोजन", "केले के लिए पानी प्रबंधन"),
        "suggest_4" to Triple("Organic pest control", "सेंद्रिय कीड नियंत्रण", "जैविक कीट नियंत्रण"),
        "llm_not_ready" to Triple("The assistant is still loading. Try again in a moment.", "सहाय्यक अजून लोड होत आहे. थोड्या वेळाने पुन्हा प्रयत्न करा.", "सहायक अभी लोड हो रहा है। थोड़ी देर में फिर कोशिश करें।"),
        "llm_empty" to Triple("No answer this time. Please rephrase your question.", "या वेळी उत्तर मिळाले नाही. प्रश्न वेगळ्या शब्दात विचारा.", "इस बार जवाब नहीं मिला। सवाल दूसरे शब्दों में पूछें।"),

        // Market tab
        "market_title" to Triple("Market and profit", "बाजार आणि नफा", "बाज़ार और मुनाफ़ा"),
        "market_sub" to Triple("Mandi prices and a per-acre profit check.", "मंडी भाव आणि प्रति एकर नफ्याची तपासणी.", "मंडी भाव और प्रति एकड़ मुनाफ़े की जाँच।"),
        "select_district" to Triple("District", "जिल्हा", "ज़िला"),
        "select_crop" to Triple("Crop", "पीक", "फसल"),
        "avg_price" to Triple("Average modal price", "सरासरी भाव", "औसत भाव"),
        "per_quintal" to Triple("per quintal", "प्रति क्विंटल", "प्रति क्विंटल"),
        "price_none" to Triple("No price data yet for this crop and district.", "या पीक व जिल्ह्यासाठी अजून भाव माहिती नाही.", "इस फसल और ज़िले के लिए अभी भाव जानकारी नहीं है।"),
        "last_synced" to Triple("Last synced", "शेवटचे अद्यतन", "अंतिम सिंक"),
        "top_markets" to Triple("Recent markets", "अलीकडील बाजार", "हाल के बाज़ार"),
        "yield_input" to Triple("Expected yield (quintal per acre)", "अपेक्षित उत्पादन (क्विंटल प्रति एकर)", "अपेक्षित उपज (क्विंटल प्रति एकड़)"),
        "cost_input" to Triple("Total cost per acre (Rs)", "प्रति एकर एकूण खर्च (रु.)", "प्रति एकड़ कुल लागत (रु.)"),
        "profit_label" to Triple("Estimated profit per acre", "प्रति एकर अंदाजे नफा", "प्रति एकड़ अनुमानित मुनाफ़ा"),
        "profit_loss" to Triple("Estimated loss per acre", "प्रति एकर अंदाजे तोटा", "प्रति एकड़ अनुमानित घाटा"),
        "profit_note" to Triple("Based on your yield and cost, and the average modal price shown. Actual prices change daily.", "तुमचे उत्पादन, खर्च आणि दाखवलेला सरासरी भाव यावर आधारित. प्रत्यक्ष भाव रोज बदलतात.", "आपकी उपज, लागत और दिखाए गए औसत भाव पर आधारित। असली भाव रोज़ बदलते हैं।")
    )

    fun t(lang: Lang, key: String): String {
        val tr = rows[key] ?: return key
        return when (lang) {
            Lang.ENGLISH -> tr.first
            Lang.MARATHI -> tr.second
            Lang.HINDI -> tr.third
        }
    }
}
