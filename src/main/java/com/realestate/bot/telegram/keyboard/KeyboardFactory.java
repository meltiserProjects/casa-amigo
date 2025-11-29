package com.realestate.bot.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Фабрика для создания inline клавиатур Telegram
 */
@Component
public class KeyboardFactory {

    /**
     * Главное меню при /start
     */
    public InlineKeyboardMarkup createMainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Ряд 1: Создать поиск
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton createSearchBtn = new InlineKeyboardButton();
        createSearchBtn.setText("🔍 Создать поиск");
        createSearchBtn.setCallbackData("CREATE_SEARCH");
        row1.add(createSearchBtn);
        keyboard.add(row1);

        // Ряд 2: Мой поиск | Помощь
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton mySearchBtn = new InlineKeyboardButton();
        mySearchBtn.setText("📋 Мой поиск");
        mySearchBtn.setCallbackData("MY_SEARCH");
        row2.add(mySearchBtn);

        InlineKeyboardButton helpBtn = new InlineKeyboardButton();
        helpBtn.setText("❓ Помощь");
        helpBtn.setCallbackData("HELP");
        row2.add(helpBtn);

        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Клавиатура выбора количества комнат
     */
    public InlineKeyboardMarkup createRoomSelection() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Ряд 1: 1 | 2 | 3
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(i + " комн.");
            btn.setCallbackData("SET_ROOMS:" + i);
            row1.add(btn);
        }
        keyboard.add(row1);

        // Ряд 2: 4 | 5+
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton btn4 = new InlineKeyboardButton();
        btn4.setText("4 комн.");
        btn4.setCallbackData("SET_ROOMS:4");
        row2.add(btn4);

        InlineKeyboardButton btn5 = new InlineKeyboardButton();
        btn5.setText("5+ комн.");
        btn5.setCallbackData("SET_ROOMS:5");
        row2.add(btn5);

        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Клавиатура выбора районов Валенсии
     */
    public InlineKeyboardMarkup createDistrictSelection(List<String> selectedDistricts) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Популярные районы Валенсии
        String[] districts = {
            "Ciutat Vella", "Ruzafa", "El Pla del Real",
            "Benimaclet", "Algirós", "Campanar",
            "L'Eixample", "Extramurs", "Poblats Marítims"
        };

        // По 2 кнопки в ряд
        for (int i = 0; i < districts.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int j = i; j < Math.min(i + 2, districts.length); j++) {
                String district = districts[j];
                InlineKeyboardButton btn = new InlineKeyboardButton();

                // Добавляем галочку если район уже выбран
                boolean isSelected = selectedDistricts != null && selectedDistricts.contains(district);
                btn.setText((isSelected ? "✅ " : "") + district);
                btn.setCallbackData("TOGGLE_DISTRICT:" + district);

                row.add(btn);
            }

            keyboard.add(row);
        }

        // Последний ряд: Все районы | Готово
        List<InlineKeyboardButton> lastRow = new ArrayList<>();

        InlineKeyboardButton allBtn = new InlineKeyboardButton();
        allBtn.setText("🌍 Все районы");
        allBtn.setCallbackData("DISTRICTS_ALL");
        lastRow.add(allBtn);

        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Готово");
        doneBtn.setCallbackData("DISTRICTS_DONE");
        lastRow.add(doneBtn);

        keyboard.add(lastRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Клавиатура управления поиском
     */
    public InlineKeyboardMarkup createSearchManagement(boolean isActive) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Ряд 1: Приостановить/Возобновить
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton pauseResumeBtn = new InlineKeyboardButton();
        if (isActive) {
            pauseResumeBtn.setText("⏸ Приостановить");
            pauseResumeBtn.setCallbackData("PAUSE_SEARCH");
        } else {
            pauseResumeBtn.setText("▶️ Возобновить");
            pauseResumeBtn.setCallbackData("RESUME_SEARCH");
        }
        row1.add(pauseResumeBtn);
        keyboard.add(row1);

        // Ряд 2: Редактировать | Удалить
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText("✏️ Редактировать");
        editBtn.setCallbackData("EDIT_SEARCH");
        row2.add(editBtn);

        InlineKeyboardButton deleteBtn = new InlineKeyboardButton();
        deleteBtn.setText("🗑 Удалить");
        deleteBtn.setCallbackData("DELETE_SEARCH");
        row2.add(deleteBtn);

        keyboard.add(row2);

        // Ряд 3: Назад
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
        backBtn.setCallbackData("BACK_TO_MAIN");
        row3.add(backBtn);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Клавиатура подтверждения удаления
     */
    public InlineKeyboardMarkup createDeleteConfirmation() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("✅ Да, удалить");
        confirmBtn.setCallbackData("CONFIRM_DELETE");
        row.add(confirmBtn);

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("CANCEL_DELETE");
        row.add(cancelBtn);

        keyboard.add(row);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Клавиатура выбора что редактировать
     */
    public InlineKeyboardMarkup createEditOptions() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Ряд 1: Цену
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton priceBtn = new InlineKeyboardButton();
        priceBtn.setText("💰 Цену");
        priceBtn.setCallbackData("EDIT_PRICE");
        row1.add(priceBtn);
        keyboard.add(row1);

        // Ряд 2: Комнаты
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton roomsBtn = new InlineKeyboardButton();
        roomsBtn.setText("🛏 Комнаты");
        roomsBtn.setCallbackData("EDIT_ROOMS");
        row2.add(roomsBtn);
        keyboard.add(row2);

        // Ряд 3: Районы
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton districtsBtn = new InlineKeyboardButton();
        districtsBtn.setText("📍 Районы");
        districtsBtn.setCallbackData("EDIT_DISTRICTS");
        row3.add(districtsBtn);
        keyboard.add(row3);

        // Ряд 4: Отмена
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("CANCEL_EDIT");
        row4.add(cancelBtn);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }
}
